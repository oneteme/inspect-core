package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InspectConfiguration.createObjectMapper;
import static org.usf.inspect.core.ReportEvent.report;
import static org.usf.inspect.core.MachineResourceMonitor.machineResourceMonitor;
import static org.usf.inspect.core.ScheduledExecutorServiceWrapper.wrap;
import static org.usf.inspect.core.StackTraceRow.exceptionStackTraceRows;

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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class TraceDispatcherHub implements TraceHub {

	private final AtomicReference<DispatchState> atomicState = new AtomicReference<>(DISABLE);
	private final ProcessingQueue<EventTrace> queue = new ProcessingQueue<>();
	
	@Getter
	private InspectCollectorConfiguration configuration;
	private EventTraceBus eventBus;
	private TraceExporter exporter;
	private ScheduledExecutorService executor;
	private AtomicBoolean dispatchNow = new AtomicBoolean();
	private int threshold;
	
	public TraceDispatcherHub configure(InspectCollectorConfiguration configuration) {
		return configure(configuration, resolveExporter(configuration.getTracing().getRemote(), configuration.isDebugMode()));
	}
	
	public TraceDispatcherHub configure(InspectCollectorConfiguration configuration, TraceExporter exporter) {
		if(!scheduling()) {
			this.configuration = configuration;
			if(configuration.isEnabled()) {
				atomicState.set(configuration.getScheduling().getState());
				this.threshold= (int) (configuration.getTracing().getQueueCapacity() *.85); //85% of the capacity
				this.exporter = exporter;
				this.eventBus = new EventTraceBus();
				if(configuration.getMonitoring().getResources().isEnabled()) {
					var rsc = configuration.getMonitoring().getResources();
					eventBus.registerHook(machineResourceMonitor(rsc));
				}
			}
		}
		else {
			reportMessage("TraceDispatcherHub.configure", "cannot reconfigure while scheduling");
		}
		return this;
	}
	
	public void start() {
		if(isEnabled()) {
			if(!scheduling()) {
				var es = newSingleThreadScheduledExecutor(daemonThread());
				var delay = configuration.getScheduling().getInterval().getSeconds(); //delay >= 10s
				this.executor = configuration.isDebugMode() ? wrap(es, false) : es;
				this.executor.scheduleWithFixedDelay(this::schedule, delay, delay, SECONDS);
				getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-hook"));
			}
			else {
				reportMessage("TraceDispatcherHub.start", "already scheduling");
			}
		}
		else { //do not throw exception, allow to start server with disabled inspect
			log.warn("tracing is disabled, traces will be lost");
		}
	}
	
	@Override
	public void emitTrace(EventTrace trace) {
		if(scheduling() && atomicState.get().canCollect() && queue.add(trace)) {
			flushIfThresholdReached();
		}
	}
	
	@Override //server usage
	public boolean emitTraces(List<EventTrace> traces) { 
		if(scheduling() && atomicState.get().canCollect() && queue.addAll(traces)) {
			flushIfThresholdReached();
			return true;
		}
		return false;
	}


	void flushIfThresholdReached(){
		if(queue.size() > threshold) {
			if(dispatchNow.compareAndSet(false, true)) { //make sure only one dispatching task is submitted
				executor.submit(()-> { //added task
					try {
						if(queue.size() > threshold) { //double check, may be dispatched by scheduled task
							log.warn("queue capacity exceeded threshold, triggering immediate dispatch...");
							dispatchTraces(false);
						}
					}
					finally {
						dispatchNow.set(false);
					}
				});
			}
			else {
				log.debug("dispatching task is already submitted");
			}
		}
	}

	@Override
	public void dispatch(InstanceEnvironment instance) { //dispatch immediately
		if(scheduling() && atomicState.get().canCollect()) {
			eventBus.triggerInstanceEmit(instance);
			exporter.dispatch(instance);
		}
		else {
			log.warn("cannot dispatch instance environment, scheduling={}, state={}", scheduling(), atomicState.get());
		}
	}
	
	void schedule() { //dispatch immediately
		try {
			eventBus.triggerSchedule();
			dispatchTraces(false);
		} catch (Throwable e) { //avoid scheduler suppression
			warnException(e, "failed to schedule dispatch");
		}
	}
	
	void dispatchTraces(boolean shutdown) { //last shutdown hook only
		try {
			if(atomicState.get().canDispatch()) {
				queue.pollAll(snp->{
					mergeSessionMaskUpdates(snp);
					var trc = snp;
					eventBus.triggerTraceDispatch(unmodifiableList(trc));
					log.trace("dispatching {} traces ..", trc.size());
					trc = exporter.dispatch(shutdown, trc);
					if(trc.isEmpty()) {
						log.trace("successfully dispatched {} items", snp.size());
					}
					else if(trc.size() < snp.size()) {
						log.warn("partially dispatched traces, {} items could not be dispatched", trc.size());
					}
					else {
						log.warn("failed to dispatch {} traces", trc.size());
					}
					return trc;
				});
			}
		} catch (Exception e) { 
			warnException(e, "failed to dispatch traces");
		}
		finally {
			try {
				removeIfCapacityExceeded(); // even not dispatched
			}
			catch (Exception e) {
				warnException(e, "failed to remove traces as capacity exceeded");
			}
		}
	}
	
	void removeIfCapacityExceeded() {
		var max = configuration.getTracing().getQueueCapacity();
		if(queue.size() > max) {
			log.warn("queue capacity exceeded, removing traces ..");
			queue.pollAll(snp->{
				try {
					var i=0;
					var arr = new Class[] {AbstractStage.class, ExceptionTrace.class, MachineResourceUsage.class, ReportEvent.class, SessionMaskUpdate.class};
					do {
						removeInstanceOf(snp, arr[i]);
					} while(++i<arr.length && snp.size() > max);
					if(snp.size() > max) {
						removeInstanceOf(snp, AbstractRequestSignal.class, AbstractRequestUpdate.class);
					}
					if(snp.size() > max) {
						removeInstanceOf(snp, AbstractSessionSignal.class, AbstractSessionUpdate.class);
					}
				}
				finally {
					if(snp.size() > max) {
						log.warn("still {} traces cannot be removed, clearing all the queue", snp.size());
						snp.clear();
						queue.setWaste(true); //disable further adding, avoid dispatch callback without initializer
					}
				}
				return snp;
			});
		}
	}
	
	static void removeInstanceOf(List<?> traces, Class<?> type) {
		var size = traces.size();
		traces.removeIf(type::isInstance);
		if(size > traces.size()) {
			log.warn("removed {} traces of type {}", size-traces.size(), type.getSimpleName());
		}
	}
	
	static void removeInstanceOf(List<?> traces, Class<? extends TraceSignal> signalType, Class<? extends TraceUpdate> updateType) {
		var size = traces.size();
		var call = traces.stream().<TraceUpdate>mapMulti((t,acc)->{
					if(updateType.isInstance(t)) {
						acc.accept(updateType.cast(t));
					}
				})
				.collect(toMap(TraceUpdate::getId, identity()));
		traces.removeIf(t-> signalType.isInstance(t) && call.containsKey(signalType.cast(t).getId()));
		traces.removeAll(call.values()); //remove callbacks after their initializers
		if(size > traces.size()) {
			log.warn("removed {} traces of type {}/{}", size-traces.size(), signalType.getSimpleName(), updateType.getSimpleName());
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
	public List<EventTrace> peek() {
		return queue.peek();
	}
	
	//require single thread executor to avoid concurrent queue access
	public <T> Future<T> peekAsync(Function<Collection<EventTrace>, T> fn){
		if(scheduling()) {
			var cf = new CompletableFuture<T>();
		    executor.submit(() -> {
		        try {
		            cf.complete(fn.apply(queue.toList()));
		        } catch (Exception e) {
		            cf.completeExceptionally(e);
		        }
		    });
		    return cf;
		}
		return completedFuture(fn.apply(queue.toList()));
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
	
	static void mergeSessionMaskUpdates(List<EventTrace> traces){
		var call = traces.stream().mapMulti((t, c)-> {
			if(t instanceof AbstractSessionUpdate sc && sc.getRequestMask().get() > 0) {
				c.accept(sc.getId());
			}
		}).collect(toSet());
		var updt = traces.stream()
			.filter(SessionMaskUpdate.class::isInstance)
			.map(SessionMaskUpdate.class::cast)
			.filter(u-> !call.contains(u.getId()))
			.collect(toMap(SessionMaskUpdate::getId, identity(), (a,b)-> a.getMask() > b.getMask() ? a : b));
		traces.removeIf(t -> t instanceof SessionMaskUpdate u && !updt.containsKey(u.getId()));
	}

	static String formatLog(String action, String msg, Throwable thwr) {
		var sb = new StringBuilder();
		sb.append("thread=").append(threadName());
		if(nonNull(action)) {
			sb.append(", action=").append(action);
		}
		if(nonNull(msg)) {
			sb.append(", message=").append(msg);
		}
		if(nonNull(thwr)) {
			sb.append(", cause=").append(thwr.getClass().getName())
			.append(":").append(thwr.getMessage());
		}
		return sb.toString();
	}

	void warnException(Throwable t, String msg, Object... args) {
		log.warn(msg, args);
		log.warn("  Caused by {} : {}", t.getClass().getSimpleName(), t.getMessage());
		if(configuration.isDebugMode()) {
			while(nonNull(t.getCause()) && t != t.getCause()) {
				t = t.getCause();
				log.warn("  Caused by {} : {}", t.getClass().getSimpleName(), t.getMessage());
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
	
	static TraceExporter resolveExporter(RemoteServerProperties rsp, boolean debug) {
		if(rsp instanceof RestRemoteServerProperties prop) {
			return new RestTraceExporter(prop, createObjectMapper(), debug);
		}
		if(isNull(rsp)) {
			log.warn("remote tracing is disabled, traces will be lost");
			return noExporter(); //no remote agent
		}
		throw new UnsupportedOperationException("unsupported remote " + rsp);
	}	
	
	static TraceExporter noExporter() {
		
		return new TraceExporter() {
			
			@Override
			public void dispatch(InstanceEnvironment env) {
				//do nothing
			}
			
			@Override
			public List<EventTrace> dispatch(boolean complete, List<EventTrace> traces) {
				return emptyList();
			}
		};
	}
}
