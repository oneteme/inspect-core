package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

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
	private final AtomicBoolean atomicRunning = new AtomicBoolean(false);

	private final TracingProperties propr;
	private final DispatcherAgent agent;
	private final List<DispatchHook> hooks;
	private final List<DispatchTask> tasks;
	private final ConcurrentLinkedSetQueue<EventTrace> queue;
	private final AtomicReference<DispatchState> atomicState;
	private int attempts;

	public EventTraceScheduledDispatcher(TracingProperties propr, SchedulingProperties schd, DispatcherAgent agent) {
		this(propr, schd, agent, emptyList());
	}

	public EventTraceScheduledDispatcher(TracingProperties propr, SchedulingProperties schd, DispatcherAgent agent, List<DispatchHook> hooks) {
		this.propr = propr;
		this.agent = agent;
		this.hooks = unmodifiableList(hooks);
		this.queue = new ConcurrentLinkedSetQueue<>();
		this.tasks = synchronizedList(new ArrayList<>());
		var delay  = schd.getInterval().getSeconds();
		this.executor.scheduleWithFixedDelay(()-> synchronizedDispatch(false, false), delay, delay, SECONDS);
		this.atomicState = new AtomicReference<>(schd.getState());
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
		return atomicState.get().canEmit() && tasks.add(task);
	}

	public boolean emit(EventTrace trace) {
		if(atomicState.get().canEmit()) {
			triggerHooks(h-> h.onTraceEmit(trace));
			dispatchIfCapacityExceeded(queue.add(trace)); 
			return true;
		}
		return false;
	}

	public boolean emitAll(Collection<EventTrace> traces) { //server usage
		if(atomicState.get().canEmit()) {
			triggerHooks(h-> h.onTracesEmit(unmodifiableCollection(traces)));
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
				executor.submit(()-> {
					log.trace("deferred dispatching traces ..");
					try {
						dispatchAll(state); 
					}
					catch (Throwable e) {
						warnException(log, e, "dispatch traces error");
					}
					finally {
						atomicRunning.set(false);
					}
					log.trace("deferred dispatch end");
				});
			}
			else {
				log.trace("scheduled dispatching traces ..");
				try {
					dispatchAll(state);
				}
				catch (Throwable e) { //catch throwable to avoid stop scheduler
					warnException(log, e, "dispatch traces error"); 
				}
				finally {
					atomicRunning.set(false);
				}
				log.trace("scheduled dispatch end");
			}
		}
	}

	void dispatchAll(DispatchState state) {
		var resolver = new EventTraceQueueManager(propr.getQueueCapacity(), propr.isModifiable(), queue);
		try {
			dispatchQueue(state, resolver);
		}
		finally {
			if(queue.size() > propr.getQueueCapacity()) {
		        log.debug("queue capacity exceeded", state);
				try {
					propagateQueue(state, resolver); //try store/reduce traces
				}
				finally {
					int n = queue.removeFrom(propr.getQueueCapacity());
					log.warn("{} last traces were deleted", n);
				}
			}
			dispatchTasks(state);
		}
	}
	
	void dispatchQueue(DispatchState state, EventTraceQueueManager resolver) {
		if(state.canPropagate()) {
			triggerHooks(DispatchHook::preDispatch);
			if(state.canDispatch()) {
				resolver.dequeue(state.wasCompleted() ? 0 : propr.getDelayIfPending(), (q, n)->{
					var traces = q;
					log.debug("dispatching {} traces .., pending {} traces", traces.size(), n);
					try {
						traces = agent.dispatch(state.wasCompleted(), ++attempts, n, traces);
						if(attempts > 5) { //more than one attempt
							log.info("successfully dispatched {} items after {} attempts", traces.size(), attempts);
						}
						attempts=0;
					} catch (Exception e) {
						throw new DispatchException(format("failed to dispatch %d traces", traces.size()), e);
					}
					finally {
						var arr = traces;
						triggerHooks(h-> h.postDispatch(q, arr));
					}
					return traces;
				});
			}
			else {
				triggerHooks(h-> h.postDispatch(emptyList(), emptyList())); 
			}
		}
	}

	void propagateQueue(DispatchState state, EventTraceQueueManager resolver){
		if(state.canPropagate()) {
			for(var h : hooks) {
				try {
					if(h.onCapacityExceeded(state.wasCompleted(), resolver)) {
						break; 
					}
				}
				catch (Exception e) { //catch exception => next hook
					warnException(log, e, "failed to execute hook '{}'", h.getClass().getSimpleName());
				}
			}
		}
	}
	
	void dispatchTasks(DispatchState state) {
		if(state.canDispatch() && !tasks.isEmpty()) {
			var arr = tasks.toArray(DispatchTask[]::new);
			for(var t : arr) { // iterator is not synchronized @see SynchronizedCollection#iterator
				try {
					t.dispatch(agent);
					tasks.remove(t);
				}
				catch (Exception e) { //catch exception => next task
					warnException(log, e, "failed to execute task '{}'", t.getClass().getSimpleName());
				}
			}
		}
	}
	
	public Stream<EventTrace> peek() {
		return queue.peek();
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
		try {
			executor.awaitTermination(5, SECONDS);
		} catch (InterruptedException e) {
			log.warn("interrupted while waiting for executor termination", e);
		}
		finally {
			synchronizedDispatch(false, true); //run it on shutdown-hook thread
		}
	}

	void triggerHooks(Consumer<DispatchHook> post){
		for(var h : hooks) {
			try {
				post.accept(h);
			}
			catch (Exception e) { //catch exception => next hook
				warnException(log, e, "failed to execute hook '{}'", h.getClass().getSimpleName());
			}
		}
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
}
