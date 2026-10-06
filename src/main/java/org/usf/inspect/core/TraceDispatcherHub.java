package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.InspectConfiguration.createObjectMapper;
import static org.usf.inspect.core.MachineResourceMonitor.machineResourceMonitor;
import static org.usf.inspect.core.ScheduledExecutorServiceWrapper.wrap;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Getter(value = AccessLevel.PROTECTED)
public class TraceDispatcherHub implements TraceHub {
	
	private final AtomicReference<DispatchState> atomicState = new AtomicReference<>(DISABLE);
	
	@Getter
	private InspectCollectorConfiguration configuration;
	private ProcessingQueue<EventTrace> queue;
	private EventTraceBus eventBus;
	private TracePublisher publisher;
	private ScheduledExecutorService executor;
	private AtomicBoolean dispatchNow = new AtomicBoolean();
	private int threshold;
	
	@Override
	public void configure(InspectCollectorConfiguration configuration) {
		configure(configuration, resolveExporter(configuration.getTracing().getRemote(), configuration.isDebugMode()));
	}
	
	public void configure(InspectCollectorConfiguration configuration, TracePublisher publiser) {
		if(!scheduling()) {
			this.configuration = configuration;
			if(configuration.isEnabled()) {
				var max = configuration.getTracing().getQueueCapacity();
				queue = new ProcessingQueue<>(max);
				atomicState.set(configuration.getScheduling().getState());
				this.threshold= (int) (max *.9); //90% of max capacity
				this.publisher = publiser;
				this.eventBus = new EventTraceBus();
				if(configuration.getMonitoring().getResources().isEnabled()) {
					var rsc = configuration.getMonitoring().getResources();
					eventBus.registerHook(machineResourceMonitor(rsc));
				}
				start();
			}
		}
		else {
			emitReport("TraceDispatcherHub.configure", "cannot reconfigure while scheduling");
		}
	}
	
	private void start() {
		if(isEnabled()) {
			if(!scheduling()) {//single thread to avoid concurrent queue access
				var es = newSingleThreadScheduledExecutor(daemonThread()); 
				var delay = configuration.getScheduling().getInterval().getSeconds(); //delay >= 10s
				this.executor = configuration.isDebugMode() ? wrap(es, false) : es;
				this.executor.scheduleWithFixedDelay(this::schedule, delay, delay, SECONDS);
				getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-hook"));
			}
			else {
				emitReport("TraceDispatcherHub.start", "already scheduling");
			}
		}
		else { //do not throw exception, allow to start server with disabled inspect
			log.warn("tracing is disabled, traces will be lost");
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
	
	void schedule() { //dispatch immediately
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
			var max = configuration.getTracing().getQueueCapacity();
			var sze = queue.size();
		    log.warn("⚠ QUEUE CAPACITY EXCEEDED: current size = {}, max capacity = {}", sze, max);
		    log.info("performing targeted trace cleanup to prevent memory leaks...");
			try {
				Class<?>[] priorityTypes = {
						AbstractStage.class, 
			            ExceptionTrace.class, 
			            MachineResourceUsage.class, 
			            ReportEvent.class, 
			            SessionEvent.class};
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
	    	    len = queue.removeIf(o -> o instanceof AbstractSessionSignal || o instanceof AbstractSessionUpdate || o instanceof SessionMaskUpdate);
	    	    log.warn(" ❌ removed {} traces of type AbstractSessionSignal/AbstractSessionUpdate/SessionMaskUpdate", len);
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
		if(isEnabled()) { //do not allow to change state if disabled => configuration is not set
			atomicState.set(state);
			return true;
		}
		return false;
	}
	
	@Deprecated(forRemoval = true, since = "1.2.0")
	public Collection<EventTrace> peek() {
		return queue.peek();
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
	
	@Override
	public boolean canCollect() {
		return scheduling() && atomicState.get().canCollect(); 
	}
	
	boolean scheduling() {
		return nonNull(executor) && !executor.isShutdown();
	}
	
	void shutdown() {
		if(scheduling()) {
			log.info("shutting down the scheduler service...");
			executor.shutdown();
			InterruptedException ie = null;
			try {
				executor.awaitTermination(30, SECONDS);
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
	}
	
	static ThreadFactory daemonThread() { //counter !?
		var counter = new AtomicInteger(0);
		return r-> {
			var thr = new Thread(r, "inspect-scheduler-" + counter.incrementAndGet());
			thr.setDaemon(true);
			thr.setUncaughtExceptionHandler((t,e)-> log.error("uncaught exception on thread {}", t.getName(), e));
			return thr;
		};
	}
	
	static TracePublisher resolveExporter(RemoteServerProperties rsp, boolean debug) {
		if(rsp instanceof RestRemoteServerProperties prop) {
			return new RestClientTracePublisher(prop, createObjectMapper(), debug);
		}
		if(isNull(rsp)) {
			log.warn("remote tracing is disabled, traces will be lost");
			return noExporter(); //no remote agent
		}
		throw new UnsupportedOperationException("unsupported remote " + rsp);
	}	
	
	static TracePublisher noExporter() {
		
		return new TracePublisher() {
			
			@Override
			public void register(InstanceEnvironment env) {
				//do nothing
			}
			
			@Override
			public void flush(boolean complete, ProcessingQueue<EventTrace> queue) {
				queue.clear();
			}
		};
	}
}
