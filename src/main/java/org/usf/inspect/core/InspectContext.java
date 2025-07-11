package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.net.InetAddress.getLocalHost;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.usf.inspect.core.InstanceType.SERVER;

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

	private final InstanceEnvironment currentInstance;
	private final EventTraceDispatcher dispatcher;
	private final ScheduledExecutorService executor;
	
	public InspectCollectorConfiguration getConfiguration() {
		return currentInstance.getConfiguration();
	}
	
	public InstanceEnvironment getCurrentInstance() {
		return currentInstance;
	}
	
	void complete() {
    	log.info("shutting down the scheduler service...");
		executor.shutdown();
		dispatcher.complete();
	}
	
	public static void emit(EventTrace trace) {
		if(nonNull(singleton)) {
			singleton.dispatcher.emit(trace);
		}
		else {
			log.warn("inspect context was not started");
		}
	}
	
	static void startInspectContext(Instant start, InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider) {
		var inst = contextInstance(start, conf, provider);
		var exct = newScheduledThreadPool(2, InspectContext::daemonThread);
		var schd = conf.getScheduling();
		var dspt = new EventTraceDispatcher();
		if(conf.getMonitoring().getResources().isEnabled()) { //important! register before other handlers
			var res = new ResourceUsageMonitor();
			dspt.registerListener(res);
			inst.setResource(res.getConfig()); //1st trace 
		}
		if(conf.isDebugMode()) {
			dspt.register(new SessionTraceDebugger());
		}
		if(conf.getTracing().getRemote() instanceof RestRemoteServerProperties rest) {
			var client = new InspectRestClient(rest, inst);
			dspt.register(new EventTraceQueueHandler(conf.getTracing(), client));
		}
		else if(nonNull(conf.getTracing().getRemote())) {
			throw new UnsupportedOperationException("unsupported remote " + conf.getTracing().getRemote());
		}
		exct.scheduleWithFixedDelay(dspt, 0, schd.getDelay(), schd.getUnit());
		singleton = new InspectContext(inst, dspt, exct);
		getRuntime().addShutdownHook(new Thread(context()::complete, "shutdown-hook"));
	}
	
	public static InspectContext context() {
		return singleton;
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
}
