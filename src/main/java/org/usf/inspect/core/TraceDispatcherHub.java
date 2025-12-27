package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.usf.inspect.core.DumpProperties.createDirs;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.LogEntry.logEntry;
import static org.usf.inspect.core.LogEntry.Level.REPORT;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.core.StackTraceRow.exceptionStackTraceRows;
import static org.usf.inspect.core.TraceExporter.noExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class TraceDispatcherHub implements TraceHub {

	private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);
	private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor(TraceDispatcherHub::daemonThread);
	
	private static TraceHub singleton;
	
	@Getter 
	private final InspectCollectorConfiguration configuration;
	private final AtomicReference<DispatchState> atomicState;
	private final TraceExporter agent;
	private final EventTraceBus eventBus;
	private final ProcessingQueue<EventTrace> queue = new ProcessingQueue<>();
	private final List<DispatchTask> tasks = synchronizedList(new ArrayList<>());
	
	private volatile boolean dispatchNow;
	
	TraceDispatcherHub(InspectCollectorConfiguration configuration, TraceExporter agent, EventTraceBus eventBus) {
		if(configuration.isEnabled()) {
			this.configuration = configuration;
			this.atomicState = new AtomicReference<>(configuration.getScheduling().getState());
			this.agent = agent;
			this.eventBus = eventBus;
			var delay = configuration.getScheduling().getInterval().getSeconds(); //delay >= 15s
			this.executor.scheduleWithFixedDelay(this::schedule, delay, delay, SECONDS);
			getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-hook"));
		}
		else {
			throw new IllegalStateException("cannot create InspectHub with disabled configuration");
		}
	}
	
	@Override
	public boolean emitTask(DispatchTask task) { //no hooks
		return scheduling() && atomicState.get().canCollect() && tasks.add(task);
	}
	
	@Override
	public boolean emitTrace(EventTrace trace) {
		if(scheduling() && atomicState.get().canCollect() && queue.add(trace)) {
			tryDispatchIfQueueFull();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean emitTraces(List<EventTrace> traces) { //server usage
		if(scheduling() && atomicState.get().canCollect() && queue.addAll(traces)) {
			tryDispatchIfQueueFull();
			return true;
		}
		return false;
	}

	void tryDispatchIfQueueFull(){
		var max = configuration.getTracing().getQueueCapacity() *.9;
		if(queue.size() > max) {
			synchronized(this) {
				if(!dispatchNow) { //make sure only one dispatching task is submitted
					dispatchNow = true;
					executor.submit(()-> { //added task
						try {
							if(queue.size() > max) { //double check, may be dispatched by scheduled task
								log.warn("queue capacity exceeded threshold, triggering immediate dispatch ..");
								dispatchTraces(false);
							}
						}
						finally {
							dispatchNow = false;
						}
					});
				}
				else {
					log.debug("dispatching task is already submitted");
				}
			}
		}
	}
	
	@Override
	public void reportError(boolean stack, String action, Throwable thwr) {
		report(stack, formatLog(action, null, thwr), thwr);
	}

	@Override
	public void reportMessage(boolean stack, String action, String msg) {
		report(stack, formatLog(action, msg, null), null);
	}
	
	void report(boolean stack, String msg, Throwable cause) {
		if(scheduling() && atomicState.get().canCollect()) {
			var arr = stack && configuration.isDebugMode() 
					? exceptionStackTraceRows(requireNonNullElseGet(cause, Exception::new), -1) 
					: null;
			queue.add(logEntry(REPORT, msg, arr)); //do not use emitTrace to avoid call hooks 
		}
		if(configuration.isDebugMode()) {
			log.debug(msg, cause);			
		}
	}

	@Override
	public boolean dispatch(InstanceEnvironment instance) { //dispatch immediately
		if(scheduling() && atomicState.get().canCollect()) {
			eventBus.triggerInstanceEmit(instance);
			try {
				agent.dispatch(instance);
				return true;
			} catch (Exception e) {
				log.warn("failed to dispatch instance {}", instance.getId());
			}
		}
		return false;
	}
	
	void schedule() { //dispatch immediately
		try {
			eventBus.triggerSchedule(this);
			dispatchTasks();
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
					eventBus.triggerTraceDispatch(this, unmodifiableList(trc));
					log.trace("dispatching {} traces ..", trc.size());
					trc = agent.dispatch(shutdown, trc);
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
			var arr = new Class[] {AbstractStage.class, MachineResourceUsage.class, LogEntry.class, SessionMaskUpdate.class};
			queue.pollAll(snp->{
				var i=0;
				do {
					var size = snp.size();
					snp.removeIf(arr[i]::isInstance);
					if(size > snp.size()) {
						log.warn("removed {} traces of type {}", size - snp.size(), arr[i].getSimpleName());
					}
				} while(++i<arr.length && snp.size() > max);
				if(snp.size() > max) {
					var size = snp.size();
					var call = snp.stream()
							.filter(TraceUpdate.class::isInstance)
							.map(TraceUpdate.class::cast)
							.collect(toMap(TraceUpdate::getId, identity()));
					snp.removeIf(t-> t instanceof TraceSignal in && !call.containsKey(in.getId()));
					snp.removeAll(call.values()); //remove callbacks after their initializers
					if(size > snp.size()) {
						log.warn("removed {} traces of type {}", size - snp.size(), "Initializer/Callback");
					}
				}
				if(snp.size() > max) {
					log.warn("still {} traces cannot be removed, clearing all the queue", snp.size());
					snp.clear();
					queue.setWaste(true); //disable further adding, avoid dispatch callback without initializer
				}
				return snp;
			});
		}
	}

	void dispatchTasks() {
		if(atomicState.get().canDispatch() && !tasks.isEmpty()) {
			var arr = tasks.toArray(DispatchTask[]::new); // iterator is not synchronized @see SynchronizedCollection#iterator
			for(var t : arr) {
				try {
					t.dispatch(agent);
					tasks.remove(t);
				}
				catch (Exception e) { //catch exception => next task
					warnException(e, "failed to execute task '{}'", t.getClass().getSimpleName());
				}
			}
		}
	}

	public DispatchState getState() {
		return atomicState.get();
	}

	public boolean setState(DispatchState state) {
		if(configuration.isEnabled() && scheduling()) { //cannot change state if disabled or shut down
			atomicState.set(state);
			return true;
		}
		return false;
	}
	
	public List<EventTrace> peek() {
		return queue.peek();
	}
	
	boolean scheduling() {
		return !executor.isShutdown();
	}
	
	void shutdown() {
		log.info("shutting down the scheduler service...");
		executor.shutdown();
		InterruptedException ie = null;
		try {
			executor.awaitTermination(5, SECONDS);
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
	
	static Thread daemonThread(Runnable r) { //counter !?
		var thread = new Thread(r, "inspect-scheduler-" + THREAD_COUNTER.incrementAndGet());
		thread.setDaemon(true);
		thread.setUncaughtExceptionHandler((t,e)-> log.error("uncaught exception on thread {}", t.getName(), e));
		return thread;
	}
	
	static synchronized void initializeTraceHub(InspectCollectorConfiguration conf, ObjectMapper mapper) {
		TraceExporter agent = null;
		if(conf.getTracing().getRemote() instanceof RestRemoteServerProperties prop) {
			agent = new RestTraceExporter(prop, mapper);
		}
		else if(isNull(conf.getTracing().getRemote())) {
			agent = noExporter(); //no remote agent
			log.warn("remote tracing is disabled, traces will be lost");
		}
		else {
			throw new UnsupportedOperationException("unsupported remote " + conf.getTracing().getRemote());
		}
		singleton = createHub(conf, agent, mapper);
	}

	public static synchronized TraceHub hub() {
		if(isNull(singleton)) {
			var config = new InspectCollectorConfiguration();
			config.setEnabled(false);
			singleton = createHub(config, null, null);
		}
		return singleton;
	}
	
	public static TraceHub createHub(InspectCollectorConfiguration conf, TraceExporter agent, ObjectMapper mapper) {
		if(conf.isEnabled()) {
			var eventBus = new EventTraceBus();
			if(conf.getMonitoring().getResources().isEnabled()) {
				log.info("machine resource monitoring is enabled");
				eventBus.registerHook(new MachineResourceMonitor(conf.getMonitoring().getResources().getDisk()));
			}
//			if(conf.isDebugMode()) {
//				bus.registerHook(new EventTraceDebugger())
//			}
			if(conf.getTracing().getDump().isEnabled()) {
				log.info("event trace dumping is enabled, location={}", conf.getTracing().getDump().getLocation());
				eventBus.registerHook(new EventTraceDumper(createDirs(conf.getTracing().getDump().getLocation(), nextId()), mapper));
			}
			return new TraceDispatcherHub(conf, agent, eventBus);
		}
		return ()-> conf;
	}
}
