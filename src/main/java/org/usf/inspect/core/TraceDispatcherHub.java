package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;
import static org.usf.inspect.util.ScheduledExecutorServiceWrapper.wrap;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.usf.inspect.AbstractRequestSignal;
import org.usf.inspect.AbstractRequestUpdate;
import org.usf.inspect.AbstractSessionSignal;
import org.usf.inspect.AbstractSessionUpdate;
import org.usf.inspect.AbstractStage;
import org.usf.inspect.EventTrace;
import org.usf.inspect.ExceptionTrace;
import org.usf.inspect.InstanceEnvironment;
import org.usf.inspect.ReportEvent;
import org.usf.inspect.SchedulingProperties;
import org.usf.inspect.SessionAsyncDurationUpdate;
import org.usf.inspect.SessionEvent;
import org.usf.inspect.SessionMaskUpdate;
import org.usf.inspect.SystemMetrics;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Getter(value = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class TraceDispatcherHub implements TraceHub {
	
	private final AtomicReference<DispatchState> atomicState = new AtomicReference<>(DISABLE);
	
	private ProcessingQueue<EventTrace> queue;
	private EventTraceBus eventBus;
	private TracePublisher publisher;
	private ScheduledExecutorService executor;
	private AtomicBoolean dispatchNow = new AtomicBoolean();
	private int threshold;
	
	public void configure(SchedulingProperties properties, TracePublisher publisher, List<DispatchHook> hooks) {
		if(!scheduling()) {
			atomicState.set(properties.getState());
			var max = properties.getQueueCapacity();
			this.queue = new ProcessingQueue<>(max);
			this.threshold= (int) (max *.9); //90% of max capacity
			this.publisher = publisher;
			this.eventBus = new EventTraceBus();
			if(nonNull(hooks)) {
				hooks.forEach(eventBus::registerHook);
			}
			var schc = newSingleThreadScheduledExecutor(daemonThread(properties.getThreadName())); 
			var delay = properties.getInterval().getSeconds(); //delay >= 10s
			this.executor = getConfiguration().getProperties().isDebugMode() ? wrap(schc, true) : schc;
			log.info("◴ scheduling traces dispatch every {} seconds", delay);
			this.executor.scheduleWithFixedDelay(this::schedule, delay, delay, SECONDS);
			getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-hook"));
		}
		else {
			REPORTER.warn("TraceDispatcherHub.configure: cannot reconfigure hub while scheduling");
		}
	}

	@Override
	public void dispatch(InstanceEnvironment instance) throws DispatchException { //dispatch immediately
		if(canCollect()) {
			eventBus.triggerInstanceEmit(instance);
			publisher.register(instance);
		}
		else {
			throw new DispatchException("cannot dispatch, hub is not enabled or scheduling is not started");
		}
	}
	
	@Override
	public void emitTrace(EventTrace trace) {
		if(canCollect() && queue.add(trace)) {
			flushIfThresholdReached();
		}
	}
	
	protected void flushIfThresholdReached(){
		if(queue.size() > threshold && dispatchNow.compareAndSet(false, true)) {  //submit task to avoid concurrent flush
			executor.submit(()-> {
				try {
					if(queue.size() > threshold) { //double check, as queue size may have changed since task submission
						log.warn("⚠ QUEUE THRESHOLD REACHED: current size = {}, threshold = {}", queue.size(), threshold);
						dispatchTraces(false);
					}
				}
				finally {
					dispatchNow.set(false);
				}
			});
		}
	}
	
	void schedule() {
		eventBus.triggerSchedule();
		dispatchTraces(false);
	}
	
	void dispatchTraces(boolean shutdown) { //last shutdown hook only
		try {
			if(atomicState.get().canDispatch()) {
				publisher.flush(shutdown, queue);
			}
		} catch (DispatchException e) {
			//do nothing
		} catch (Throwable e) { //unexpected error, do not throw exception to avoid scheduler shutdown
			log.error("unexpected error while dispatching traces", e);
		}
		finally {
			removeIfCapacityExceeded(); // even not dispatched
		}
	}
	
	void removeIfCapacityExceeded() {
		if(queue.isCapacityExceeded()) {
			var sze = queue.size();
			var max = queue.getMaxCapacity();
		    log.warn("⚠ QUEUE CAPACITY EXCEEDED: current size = {}, max capacity = {}", sze, max);
		    log.info("performing targeted trace cleanup to prevent memory leaks...");
			try {
				Class<?>[] priorityTypes = {
						AbstractStage.class, 
			            ExceptionTrace.class, 
			            SystemMetrics.class,
			            SessionMaskUpdate.class,
			            SessionAsyncDurationUpdate.class,
			            SessionEvent.class, 
			            ReportEvent.class};
		        for(Class<?> clazz : priorityTypes) {
		    	    var len = queue.removeIf(clazz::isInstance);
		            log.warn(" ❌ removed {} traces of type {}", len, clazz.getSimpleName());
		            if (queue.size() < max) {
		                return;
		            }
		        }
	    	    var len = queue.removeIf(o -> o instanceof AbstractRequestSignal || o instanceof AbstractRequestUpdate);
	    	    log.warn(" ❌ removed {} traces of type AbstractRequestSignal/AbstractRequestUpdate", len);
	    	    if (queue.size() < max) {
	                return;
	            }
	    	    len = queue.removeIf(o -> o instanceof AbstractSessionSignal || o instanceof AbstractSessionUpdate);
	    	    log.warn(" ❌ removed {} traces of type AbstractSessionSignal/AbstractSessionUpdate", len);
			}
			catch (Throwable e) {
				log.error("💥 CRITICAL: failed to remove traces as capacity exceeded", e);
			}
			if(queue.size() >= max) {
				log.warn(" ❌ removing all remaining {} traces, as queue size still exceeds max capacity", queue.size());
				queue.clear();
			}
		}
	}
	
	public DispatchState getState() {
		return atomicState.get();
	}

	public boolean setState(DispatchState state) {
		if(scheduling()) { //do not allow state change if scheduling is not started
			atomicState.set(state);
			return true;
		}
		return false;
	}
	
	//require single thread executor to avoid concurrent queue access
	public <T> Future<T> peekAsync(Function<Collection<EventTrace>, T> fn){
		if(scheduling()) {
			var cf = new CompletableFuture<T>();
		    executor.submit(() -> {
		        try {
		            cf.complete(fn.apply(queue.peek()));
		        } catch (Exception e) {
		            cf.completeExceptionally(e);
		        }
		    });
		    return cf;
		}
		return completedFuture(fn.apply(queue.peek()));
	}
	
	protected boolean canCollect() {
		return scheduling() && atomicState.get().canCollect(); 
	}
	
	boolean scheduling() {
		return nonNull(executor) && !executor.isShutdown();
	}
	
	void shutdown() {
		if(scheduling()) {
			log.info("⏻ shutting down the scheduler service...");
			executor.shutdown();
			InterruptedException ie = null;
			try {
				executor.awaitTermination(10, SECONDS);
			} catch (InterruptedException e) { // shutting down host
				log.warn("interrupted while waiting for executor termination", e);
				ie = e;
			}
			finally { //final dispatch, will be executed on shutdown hook thread
				dispatchTraces(true);
				if(nonNull(ie)) {
					currentThread().interrupt();
				}
			}
		}
		else {
			log.warn("scheduler service is not running, cannot shutdown");
		}
	}
	
	static ThreadFactory daemonThread(String threadname) { //counter !?
		var counter = new AtomicInteger(0);
		return r-> {
			var thr = new Thread(r, threadname + "-" + counter.incrementAndGet());
			thr.setDaemon(true);
			thr.setUncaughtExceptionHandler((t,e)-> log.error("uncaught exception on thread {}", t.getName(), e));
			return thr;
		};
	}
}
