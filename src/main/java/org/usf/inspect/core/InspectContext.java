package org.usf.inspect.core;

import static java.lang.Math.max;
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
import static org.usf.inspect.core.DispatcherAgent.noAgent;
import static org.usf.inspect.core.DumpProperties.createDirs;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.LogEntry.logEntry;
import static org.usf.inspect.core.LogEntry.Level.REPORT;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.core.StackTraceRow.exceptionStackTraceRows;

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
public final class InspectContext implements Context {

	private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);
	private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor(InspectContext::daemonThread);
	
	private static Context singleton;
	
	@Getter 
	private final InspectCollectorConfiguration configuration;
	private final AtomicReference<DispatchState> atomicState;
	private final DispatcherAgent agent;
	private final EventTraceBus eventBus;
	private final ProcessingQueue<EventTrace> queue = new ProcessingQueue<>();
	private final List<DispatchTask> tasks = synchronizedList(new ArrayList<>());
	
	private volatile boolean dispatching;
	
	InspectContext(InspectCollectorConfiguration configuration, DispatcherAgent agent, EventTraceBus eventBus) {
		if(!configuration.isEnabled()) {
			throw new IllegalStateException("cannot create InspectContext with disabled configuration");
		}
		this.configuration = configuration;
		this.atomicState = new AtomicReference<>(configuration.getScheduling().getState());
		this.agent = agent;
		this.eventBus = eventBus;
		var delay = configuration.getScheduling().getInterval().getSeconds(); //delay >= 15s
		this.executor.scheduleWithFixedDelay(()-> schedule(false), delay, delay, SECONDS);
		getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-hook"));
	}
	
	@Override
	public boolean emitTask(DispatchTask task) { //no hooks
		return canCollect() && tasks.add(task);
	}
	
	@Override
	public boolean emitTrace(EventTrace trace) {
		if(canCollect() && queue.add(trace)) {
			triggerImmediateDispatchIfQueueFull();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean emitTraces(List<EventTrace> traces) { //server usage
		if(canCollect() && queue.addAll(traces)) {
			triggerImmediateDispatchIfQueueFull();
			return true;
		}
		return false;
	}

	void triggerImmediateDispatchIfQueueFull(){
		var max = configuration.getTracing().getQueueCapacity() *.9;
		if(scheduling() && queue.size() > max) {
			synchronized(this) {
				if(!dispatching) {
					dispatching = true;
					executor.submit(()-> { //added task
						try {
							if(queue.size() > max) { //double check, may be dispatched by scheduled task
								log.info("queue capacity exceeded threshold, triggering immediate dispatch ..");
								dispatchTraces(false);
							}
						}
						finally {
							dispatching = false;
						}
					});
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
		if(canCollect()) {
			var arr = stack && configuration.isDebugMode() 
					? exceptionStackTraceRows(requireNonNullElseGet(cause, Exception::new), -1) 
					: null;
			queue.add(logEntry(REPORT, msg, arr)); //do not use emitTrace to avoid call hooks 
		}
		else { //debug mode
			log.warn(msg);
		}
	}

	@Override
	public boolean dispatch(InstanceEnvironment instance) { //dispatch immediately
		if(canDispatch()) {
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
	
	void schedule(boolean last) { //dispatch immediately
		try {
			if(canCollect()) {
				eventBus.triggerSchedule(this);
			}
			dispatchTasks();
			dispatchTraces(last);
		} catch (Throwable e) { //avoid scheduler suppression
			warnException(e, "failed to schedule dispatch");
		}
	}
	
	void dispatchTraces(boolean last) { //last shutdown hook only
		try {
			if(canDispatch()) {
				var max = configuration.getTracing().getQueueCapacity();
				queue.pollAll(max, snp->{
					mergeSessionMaskUpdates(snp);
					var trc = snp;
					eventBus.triggerTraceDispatch(this, unmodifiableList(trc));
					log.trace("dispatching {} traces .., pending {} traces", trc.size(), 0);
					trc = agent.dispatch(last, trc);
					if(trc.isEmpty()) {
						log.trace("successfully dispatched {} items", trc.size());
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
			removeIfCapacityExceeded(); // even not dispatched
		}
	}
	
	void removeIfCapacityExceeded() {
		var max = configuration.getTracing().getQueueCapacity();
		if(queue.size() > max) {
			var arr = new Class[] {AbstractStage.class, MachineResourceUsage.class, LogEntry.class, SessionMaskUpdate.class};
			queue.pollAll(max, snp->{
				for(var typ : arr) {
					var size = snp.size();
					if(size > max) {
						snp.removeIf(typ::isInstance);
						if(size > snp.size()) {
							log.warn("removed {} traces of type {}", size - snp.size(), typ.getSimpleName());
						}
					}
					else {
						break;
					}
				}
				return snp;
			});
		}
	}

	void dispatchTasks() {
		if(canDispatch() && !tasks.isEmpty()) {
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
	
	public boolean canCollect() {
		return scheduling() && atomicState.get().canCollect();
	}
	
	public boolean canDispatch() {
		return scheduling() && atomicState.get().canDispatch();
	}

	public DispatchState getState() {
		return atomicState.get();
	}

	public boolean setState(DispatchState state) {
		if(scheduling()) {
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
		var updates = traces.stream()
		.filter(SessionMaskUpdate.class::isInstance)
		.map(SessionMaskUpdate.class::cast)
		.collect(toMap(SessionMaskUpdate::getId, identity(), (a,b)-> a.getMask() > b.getMask() ? a : b));
		traces.removeIf(t -> {
	        if (t instanceof SessionMaskUpdate) {
	        	return true;
	        }
	        if (t instanceof AbstractSessionCallback ses) {
	            var upd = updates.get(ses.getId());
	            if (upd != null) {
	                ses.getRequestMask().updateAndGet(v -> max(v, upd.getMask()));
	            }
	        }
	        return false;
	    });
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

	static void warnException(Throwable t, String msg, Object... args) {
		log.warn(msg, args);
		log.warn("  Caused by {} : {}", t.getClass().getSimpleName(), t.getMessage());
		if(log.isDebugEnabled()) {
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
	
	static synchronized void initializeInspectContext(InspectCollectorConfiguration conf, ObjectMapper mapper) {
		DispatcherAgent agent = null;
		if(conf.getTracing().getRemote() instanceof RestRemoteServerProperties prop) {
			agent = new RestDispatcherAgent(prop, mapper);
		}
		else if(isNull(conf.getTracing().getRemote())) {
			agent = noAgent(); //no remote agent
			log.warn("remote tracing is disabled, traces will be lost");
		}
		else {
			throw new UnsupportedOperationException("unsupported remote " + conf.getTracing().getRemote());
		}
		singleton = createContext(conf, agent, mapper);
	}

	public static synchronized Context context() {
		if(isNull(singleton)) {
			var config = new InspectCollectorConfiguration();
			config.setEnabled(false);
			singleton = createContext(config, null, null);
		}
		return singleton;
	}
	
	public static Context createContext(InspectCollectorConfiguration conf, DispatcherAgent agent, ObjectMapper mapper) {
		if(conf.isEnabled()) {
			var eventBus = new EventTraceBus();
			if(conf.getMonitoring().getResources().isEnabled()) {
				eventBus.registerHook(new MachineResourceMonitor(conf.getMonitoring().getResources().getDisk()));
			}
//			if(conf.isDebugMode()) {
//				bus.registerHook(new EventTraceDebugger())
//			}
			if(conf.getTracing().getDump().isEnabled()) {
				eventBus.registerHook(new EventTraceDumper(createDirs(conf.getTracing().getDump().getLocation(), nextId()), mapper));
			}
			return new InspectContext(conf, agent, eventBus);
		}
		return ()-> conf;
	}
}
