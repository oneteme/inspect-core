package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.time.Instant.MIN;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.usf.inspect.core.BasicDispatchState.DISPATCH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class EventTraceScheduledDispatcher {

	private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor(EventTraceScheduledDispatcher::daemonThread);
	private final AtomicReference<DispatchState> atomicState = new AtomicReference<>(DISPATCH);
	private final AtomicBoolean atomicRunning = new AtomicBoolean(false);

	private final TracingProperties propr;
	private final DispatcherAgent agent;
	private final List<DispatchHook> hooks;
	private final List<DispatchTask> tasks;
	private final ConcurrentLinkedSetQueue<EventTrace> queue;
	private int attempts;

	public EventTraceScheduledDispatcher(TracingProperties prop, SchedulingProperties schd, DispatcherAgent agent) {
		this(prop, schd, agent, emptyList());
	}

	public EventTraceScheduledDispatcher(TracingProperties propr, SchedulingProperties schd, DispatcherAgent agent, List<DispatchHook> hooks) {
		this.propr = propr;
		this.agent = agent;
		this.hooks = unmodifiableList(hooks);
		this.queue = new ConcurrentLinkedSetQueue<>();
		this.tasks = synchronizedList(new ArrayList<>());
		var delay = schd.getInterval().getSeconds();
		this.executor.scheduleWithFixedDelay(()-> synchronizedDispatch(false, false), delay, delay, SECONDS);
		getRuntime().addShutdownHook(new Thread(this::complete, "shutdown-hook"));
	}

	public boolean dispatch(InstanceEnvironment instance) { //dispatch immediately
		if(atomicState.get().canDispatch()) {
			triggerHooks(h-> h.onInstanceEmit(instance));
			try {
				agent.dispatch(instance);
				return true;
			} catch (Exception e) {
				warnException(log, e, "failed to dispatch instance {}", instance.getId());
			}
		}
		return false;
	}

	public boolean emit(DispatchTask task) {
		if(atomicState.get().canEmit()) {
			return tasks.add(task);
		}
		return false;
	}

	public boolean emit(EventTrace trace) {
		if(atomicState.get().canEmit()) {
			triggerHooks(h-> h.onTraceEmit(trace));
			dispatchIfCapacityExceeded(queue.add(trace)); 
			return true;
		}
		return false;
	}

	public boolean emitAll(EventTrace[] traces) { //server usage
		if(atomicState.get().canEmit()) {
			triggerHooks(h-> h.onTracesEmit(traces));
			dispatchIfCapacityExceeded(queue.addAll(traces));
			return true;
		}
		return false;
	}

	private void dispatchIfCapacityExceeded(int size){
		if(size > propr.getQueueCapacity()) {
			synchronizedDispatch(true, false); //deferred process
		}
	}

	void synchronizedDispatch(boolean deferred, boolean complete) {
		if(!atomicRunning.compareAndExchange(false, true) || complete) { //complete => force dispatch, deferred => after trace emit
			var state = atomicState.get();
			if(deferred) {
				log.trace("deferred dispatching traces ..");
				executor.submit(()-> safeDispatch(state, ()->{
					atomicRunning.set(false);
					log.trace("deferred dispatch end");
				}));
			}
			else {
				log.trace("scheduled dispatching traces ..");
				safeDispatch(state, ()->{
					atomicRunning.set(false);
					log.trace("scheduled dispatch end");
				});
			}
		}
	}

	void safeDispatch(DispatchState state, Runnable after) {
		try {
			dispatchQueue(state);
		}
		catch (Throwable e) {
			warnException(log, e, "dispatch traces error"); //do not log exception stack trace
		}
		if(queue.size() > propr.getQueueCapacity()) {
	        log.info("Queue capacity exceeded: removed last {} traces");
			try {
				propagateTraces(state);
			}
			catch (Throwable e) {
				warnException(log, e, "propagate traces error"); //do not log exception stack trace
			}
			int n = queue.removeFrom(propr.getQueueCapacity());
			log.debug("{} last traces were deleted", n);
		}
		try {
			dispatchTasks(state);
		}
		catch (Throwable e) {
			warnException(log, e, "dispatch tasks error"); //do not log exception stack trace
		}
		after.run();
	}
	
	void dispatchTasks(DispatchState state) {
		if(state.canDispatch() && !tasks.isEmpty()) {
			var arr = tasks.toArray(DispatchTask[]::new);
			for(var t : arr) { // iterator is not synchronized @see SynchronizedCollection.iterator
				try {
					t.dispatch(agent);
					tasks.remove(t);
				}
				catch (Exception e) {
					throw new DispatchException("dispatch task error", e);
				}
			}
		}
	}

	void propagateTraces(DispatchState state){
		if(state.canPropagate()) {
			dequeue(state.wasCompleted() ? 0 : -1, (trc, pnd, que)->{
				var arr = trc.toArray(EventTrace[]::new);
				for(var h : hooks) {
					try {
						if(h.onCapacityExceeded(arr)) {
							return pnd; 
						}
					}
					catch (Exception e) { //catch exception => next hook
						warnException(log, e, "failed to execute hook '{}'", h.getClass().getSimpleName());
					}
				}
				removeMinorTraces(que); //change on queue
				return que;
			});
		}
		else {
			log.warn("cannot propargate traces as the dispatcher state is {}", state);
		}
	}
	
	void removeMinorTraces(Collection<EventTrace> queue) {
		var size = queue.size(); // 1- remove all non complete traces
		if(size > propr.getQueueCapacity()) {
			for(var it=queue.iterator(); it.hasNext();) { 
				var t = it.next();
				if(t instanceof CompletableMetric cm) {
					cm.runSynchronizedIfNotComplete(it::remove);  
				}
			} 
			log.debug("{} non-complete traces were deleted", size - queue.size());
			size = queue.size(); // 2- remove resource usage 
			if(size > propr.getQueueCapacity()) {
				queue.removeIf(t-> t instanceof MachineResourceUsage);
				log.debug("{} resource usage traces were deleted", size-queue.size());
				size = queue.size(); // 3- remove all stages
				if(size > propr.getQueueCapacity()) {
					queue.removeIf(t-> t instanceof AbstractStage);
					log.debug("{} stage traces were deleted", size-queue.size());
					size = queue.size(); // 4- remove all logs
					if(size > propr.getQueueCapacity()) {
						queue.removeIf(t-> t instanceof LogEntry);
						log.debug("{} log traces were deleted", size-queue.size());
					}
				}
			}
		}
	}

	void dispatchQueue(DispatchState state) {
		if(state.canPropagate()) {
			dequeue(state.wasCompleted() ? 0 : propr.getDelayIfPending(), (trc, pnd, que)->{
				var arr = trc.toArray(EventTrace[]::new);
				triggerHooks(h-> h.onDispatch(state.wasCompleted(), arr));
				if(state.canDispatch()) {
					try {
						log.debug("dispatching {}/{} traces ..", trc.size(), que.size());
						agent.dispatch(state.wasCompleted(), ++attempts, pnd.size(), arr);
						if(attempts > 5) { //more than one attempt
							log.info("successfully dispatched {} items after {} attempts", trc.size(), attempts);
						}
						attempts=0;
						return pnd; //requeue pending traces
					} catch (Exception e) {
						throw new DispatchException(format("failed to dispatch %d traces", trc.size()), e); //do not log exception stack trace
					}
				}
				else {
					log.warn("cannot dispatch traces as the dispatcher state is {}", state);
				}
				return que;
			});
		}
		else {
			log.warn("cannot propargate traces as the dispatcher state is {}", state);
		}
	}

	void dequeue(int delay, QueueConsumer cons) {
		var trc = queue.pop(); //read only
		try {
			var edt = new ArrayList<>(trc);
			var pnd = extractPendingTrace(edt, delay); // 0: takes all, -1: completed only, 
			trc = cons.accept(edt, pnd, trc);
		}
		catch (OutOfMemoryError e) {
			trc = emptyList(); //do not add items back to the queue, may release memory
			log.error("out of memory error while queue processing, {} traces will be aborted", trc.size());
			throw e;
		}
		finally {
			if(!trc.isEmpty()) {
				queue.requeueAll(trc); //go back to the queue (preserve order)
			}
		}
	}

	List<EventTrace> extractPendingTrace(List<EventTrace> queue, int delay) {
		var arr = new ArrayList<EventTrace>();
		if(delay != 0) { //else keep all traces
			var start = delay > -1 ? now().minusSeconds(delay) : MIN;
			if(!queue.isEmpty()){
				for(var it=queue.listIterator(); it.hasNext();) {
					if(it.next() instanceof CompletableMetric mtr) {
						mtr.runSynchronizedIfNotComplete(()-> {
							if(mtr.getStart().isBefore(start)) {
								it.set(mtr.copy()); //send copy, avoid dispatch same reference
								log.trace("completable trace pending since {}, dequeued: {}", mtr.getStart(),  mtr);
							}
							else {
								arr.add(mtr);
								it.remove();
								log.trace("completable trace pending since {}, kept in queue: {}", mtr.getStart(),  mtr);
							}
						});
					}
				}
			}
		}
		return arr;
	}

	public DispatchState getState() {
		return atomicState.get();
	}

	public void setState(BasicDispatchState state) {
		atomicState.set(state);
	}

	void complete() {
		atomicState.getAndUpdate(DispatchState::complete);
		log.info("shutting down the scheduler service...");
		executor.shutdown();
		synchronizedDispatch(false, true);
	}

	public Stream<EventTrace> peek() {
		return queue.peek();
	}

	void triggerHooks(Consumer<DispatchHook> post){
		for(var h : hooks) {
			try {
				post.accept(h);
			}
			catch (Exception e) {
				warnException(log, e, "failed to execute hook '{}'", h.getClass().getSimpleName());
			}
		}
	}

	static boolean isEmpty(List<?> arr) {
		return isNull(arr) || arr.isEmpty();
	}

	static Thread daemonThread(Runnable r) { //counter !?
		var thread = new Thread(r, "inspect-dispatcher");
		thread.setDaemon(true);
		thread.setUncaughtExceptionHandler((t,e)-> log.error("uncaught exception on thread {}", t.getName(), e));
		return thread;
	}

	static void warnException(Logger log, Throwable t, String msg, Object... args) {
		log.warn(msg, args);
		log.warn("  Caused by {} : {}", t.getClass().getSimpleName(), t.getMessage());
		if(log.isDebugEnabled()) {
			while(nonNull(t.getCause()) && t != t.getCause()) {
				t = t.getCause();
				log.warn("  Caused by {} : {}", t.getClass().getSimpleName(), t.getMessage());
			}
		}
	}

	static interface QueueConsumer {

		Collection<EventTrace> accept(List<EventTrace> traces, List<EventTrace> pending, Collection<EventTrace> queue);
	}
}
