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
		if(size > propr.getQueueCapacity() * 0.9) {
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
			}
		}
	}

	void dispatchAll(DispatchState state) {
		try {
			if(state.canPropagate()) {
				triggerHooks(DispatchHook::preDispatch);
				try {
					dispatchQueue(state);
				}
				finally {
					triggerHooks(DispatchHook::postDispatch);
				}
			}
		}
		finally {
			dispatchTasks(state);
		}
	}
	
	void dispatchQueue(DispatchState state) {
		if(state.canDispatch()) {
			queue.safeConsume(propr.getQueueCapacity(), snp->{
				var trc = snp;
				log.trace("dispatching {} traces .., pending {} traces", trc.size(), 0);
				try {
					trc = agent.dispatch(state.wasCompleted(), ++attempts, 0, trc);
					log.trace("successfully dispatched {} items after {} attempts", trc.size(), attempts);
					attempts=0;
				} catch (Exception e) {
					var max = propr.getQueueCapacity();
					if(trc.size() > max) {
						deletedTraces(trc, max, MachineResourceUsage.class, AbstractStage.class, LogEntry.class, Callback.class); //delete Exception
						if(trc.size() > max) { 
							trc = emptyList(); //DANGER
//							atomicState.set(DISABLE); //TODO stop tracing ! server
						}
					}
					throw new DispatchException(format("failed to dispatch %d traces", trc.size()), e);
				}
				return trc;
			});
		}
	}
	
	void deletedTraces(Collection<EventTrace> traces, int maxCapacity, Class<?>... types) {
		for(var t : types) {
			var size = traces.size();
			if(size > maxCapacity) {
				traces.removeIf(t::isInstance);
				if(size > traces.size()) {
					log.warn("{} {} traces were deleted", size - traces.size(), t.getSimpleName());
				}
			}
			else {
				break;
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
	
	public List<EventTrace> peek() {
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
		InterruptedException ie;
		try {
			executor.awaitTermination(5, SECONDS);
		} catch (InterruptedException e) { // shutting down host
			log.warn("interrupted while waiting for executor termination", e);
			ie = e;
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
