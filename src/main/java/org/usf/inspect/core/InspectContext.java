package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.net.InetAddress.getLocalHost;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.usf.inspect.core.InstanceType.SERVER;
import static org.usf.inspect.core.LogEntry.logEntry;
import static org.usf.inspect.core.LogEntry.Level.ERROR;

import java.net.UnknownHostException;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class InspectContext {

	static InspectContext singleton;

	private final InspectCollectorConfiguration configuration;
	private final EventTraceEmitter eventEmitter; //optional, can be null
	private final ScheduledExecutorService executor; //optional, can be null
	
	public static InspectContext context() {
		if(isNull(singleton)) {
			singleton = new InspectContext(disabledConfiguration(), null, null);
			log.warn("", new IllegalStateException("inspect context was not started"));
		}
		return singleton;
	}
	
	public InspectCollectorConfiguration getConfiguration() {
		return configuration;
	}

	public void reportError(String msg, Throwable thrw) { //stack trace ??
		msg += format(", cause=%s: %s", thrw.getClass().getSimpleName(), thrw.getMessage());
		emitTrace(logEntry(ERROR, msg, thrw, 10)); //takes no session id
	}

	public void reportError(String msg) {
		emitTrace(logEntry(ERROR, msg, 10)); //takes no session id 
	}
	
	public void emitTrace(EventTrace trace) {
		if(nonNull(eventEmitter)) {
			eventEmitter.emitTrace(trace);
		}
	}
	
	void complete() {
    	log.info("shutting down the scheduler service...");
    	if(nonNull(executor)) {
    		executor.shutdown();
		}
    	if(nonNull(eventEmitter)) {
			eventEmitter.complete();
    	}
	}
	
	static void startInspectContext(Instant start, InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider) {
		var inst = contextInstance(start, conf, provider);
		var exct = newSingleThreadScheduledExecutor(InspectContext::daemonThread);
		var schd = conf.getScheduling();
		var dspt = new EventTraceEmitter();
		if(conf.getMonitoring().getResources().isEnabled()) {
			var res = new ResourceUsageMonitor();
			dspt.addListener(res); //important! register before other handlers
			inst.setResource(res.startupResource()); //1st trace 
		}
		if(conf.isDebugMode()) {
			dspt.register(new EventTraceDebugger());
		}
		if(conf.getTracing().getRemote() instanceof RestRemoteServerProperties prop) {
			var client = new EventTraceRestDispatcher(prop, inst);
			dspt.register(new EventTraceQueueHandler(conf.getTracing(), client));
		}
		else if(nonNull(conf.getTracing().getRemote())) {
			throw new UnsupportedOperationException("unsupported remote " + conf.getTracing().getRemote());
		}
		exct.scheduleWithFixedDelay(dspt, 0, schd.getDelay(), schd.getUnit());
		singleton = new InspectContext(conf, dspt, exct);
		getRuntime().addShutdownHook(new Thread(singleton::complete, "shutdown-hook"));
	}
	
	static Thread daemonThread(Runnable r) {
		var thread = new Thread(r, "inspect-dispatcher");
 		thread.setDaemon(true);
 		thread.setUncaughtExceptionHandler((t,e)-> log.error("uncaught exception", e));
		return thread;
	}
	
    static InstanceEnvironment contextInstance(Instant start, InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider) {
    	return new InstanceEnvironment(
    			start, SERVER,
    			provider.getName(), 
    			provider.getVersion(),
    			provider.getEnvironment(),
				hostAddress(),
				getProperty("os.name"), //version ? window 10 / Linux
				"java/" + getProperty("java.version"),
				getProperty("user.name"),
				provider.getBranch(),
				provider.getCommitHash(),
				collectorID(),
				provider.additionalProperties(),
				conf);
	}

	static String hostAddress() {
		try {
			return getLocalHost().getHostAddress(); //hostName ?
		} catch (UnknownHostException e) {
			log.warn("error while getting host address {}", e.getMessage());
			return null;
		}
	}
	
	static String collectorID() {
		return "spring-collector/" //use getImplementationTitle
				+ requireNonNullElse(InstanceEnvironment.class.getPackage().getImplementationVersion(), "?");
	}
	
	static InspectCollectorConfiguration disabledConfiguration() {
		var conf = new InspectCollectorConfiguration();
		conf.setEnabled(false);
		conf.setDebugMode(false);
		conf.getTracing().setRemote(null); //avoid remote dispatching
		//conf.getScheduling().setDelay(0); //avoid scheduling
		conf.getMonitoring().getResources().setEnabled(false); //avoid resource monitoring
		conf.getMonitoring().getException().setMaxStackTraceRows(0); //avoid memory leak
		conf.getMonitoring().getException().setMaxCauseDepth(0); //avoid memory leak
		return conf;
	}
}
