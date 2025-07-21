package org.usf.inspect.core;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.net.InetAddress.getLocalHost;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.DispatcherAgent.noAgent;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InstanceType.SERVER;
import static org.usf.inspect.core.LogEntry.logEntry;
import static org.usf.inspect.core.LogEntry.Level.ERROR;
import static org.usf.inspect.core.MainSessionType.STARTUP;
import static org.usf.inspect.core.SessionManager.createStartupSession;
import static org.usf.inspect.core.SessionManager.emitStartupSesionEnd;
import static org.usf.inspect.core.SessionManager.emitStartupSession;

import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;

import org.usf.inspect.core.Dispatcher.DispatchHook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

	private static final ObjectMapper mapper = createObjectMapper(); //json mapper, used for serialization

	private static InspectContext singleton;

	private final InspectCollectorConfiguration configuration;
	private final EventTraceDispatcher dispatcher;
	
	private MainSession session;
	
	public static InspectContext context() {
		if(isNull(singleton)) {
			singleton = new InspectContext(disabledConfiguration(), null);
			log.warn("", new IllegalStateException("inspect context was not started"));
		}
		return singleton;
	}
	
	public static ObjectMapper defaultObjectMapper() {
		return mapper;
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
		if(nonNull(dispatcher)) {
			dispatcher.emit(trace);
		}
	}

    void traceStartupSession(Instant instant) {
		var ses = createStartupSession();
		ses.setType(STARTUP.name());
    	ses.setName("main");
    	ses.setStart(instant);
    	ses.setThreadName(threadName());
    	emitStartupSession(ses);
    	this.session = ses;
    }

	void traceStartupSession(Instant instant, String clazz, Throwable t) {
    	session.runSynchronized(()-> {
			session.setLocation(clazz);
			if(nonNull(t)) {  //nullable
				session.setException(fromException(t));
			}
			session.setEnd(instant);
		});
    	emitStartupSesionEnd(session);
    	this.session = null; //prevent further usage
	}
	
	static void initializeInspectContext(Instant start, InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider) {
		var hooks = new ArrayList<DispatchHook>();
		if(conf.getMonitoring().getResources().isEnabled()) {
			hooks.add(new ResourceUsageMonitor()); //important! register before other hooks
		}
		if(conf.isDebugMode()) {
			hooks.add(new EventTraceDebugger());
		}
		DispatcherAgent agnt = null;
		if(conf.getTracing().getRemote() instanceof RestRemoteServerProperties prop) {
			agnt = new RestDispatcherAgent(prop);
			hooks.add(new EventTraceDumper(mapper, conf.getTracing().getDumpDirectory()));
			hooks.add(new EventTracePurger(conf.getTracing().getQueueCapacity()));
		}
		else if(nonNull(conf.getTracing().getRemote())) {
			throw new UnsupportedOperationException("unsupported remote " + conf.getTracing().getRemote());
		}
		else {
			agnt = noAgent(); //no remote agent
			log.warn("remote tracing is disabled, traces will be lost");
		}
		var dspt = new EventTraceDispatcher(conf.getTracing(), conf.getScheduling(), agnt, hooks);
		dspt.initialize(contextInstance(start, conf, provider));
		singleton = new InspectContext(conf, dspt);
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
	
	static ObjectMapper createObjectMapper() {
		var mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule()); //new ParameterNamesModule() not required, read only
		mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		//mapper.disable(WRITE_DATES_AS_TIMESTAMPS) important! write Instant as double
		mapper.registerSubtypes(
			new NamedType(LogEntry.class, 				"log"),  
			new NamedType(MachineResourceUsage.class, 	"rsrc-usg"),
			new NamedType(MainSession.class,  			"main-ses"), 
			new NamedType(RestSession.class,  			"rest-ses"), 
			new NamedType(LocalRequest.class, 			"locl-req"), 
			new NamedType(DatabaseRequest.class,		"jdbc-req"),
			new NamedType(RestRequest.class,  			"http-req"), 
			new NamedType(MailRequest.class,  			"mail-req"), 
			new NamedType(NamingRequest.class,			"ldap-req"), 
			new NamedType(FtpRequest.class,  			"ftp-req"),
			new NamedType(DatabaseRequestStage.class,	"jdbc-stg"),
			new NamedType(HttpRequestStage.class,  		"http-stg"), 
			new NamedType(MailRequestStage.class,  		"mail-stg"), 
			new NamedType(NamingRequestStage.class,		"ldap-stg"), 
			new NamedType(FtpRequestStage.class,  		"ftp-stg"));
		return mapper;
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

	static Thread daemonThread(Runnable r) {
		var thread = new Thread(r, "inspect-dispatcher");
 		thread.setDaemon(true);
 		thread.setUncaughtExceptionHandler((t,e)-> log.error("uncaught exception", e));
		return thread;
	}
}
