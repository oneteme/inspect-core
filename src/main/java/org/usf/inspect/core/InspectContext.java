package org.usf.inspect.core;

import static java.lang.System.getProperty;
import static java.net.InetAddress.getLocalHost;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;
import static org.usf.inspect.core.BasicDispatchState.DISABLE;
import static org.usf.inspect.core.DispatcherAgent.noAgent;
import static org.usf.inspect.core.DumpProperties.createDirs;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.InstanceType.SERVER;
import static org.usf.inspect.core.SessionContextManager.createStartupSession;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.core.SessionContextManager.reportSessionIsNull;

import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
	private final EventTraceScheduledDispatcher dispatcher;

	private SessionContext sesCtx;

	public static InspectContext context() {
		if(isNull(singleton)) {
			singleton = new InspectContext(disabledConfiguration(), null);
			log.warn("inspect context was not started");
		}
		return singleton;
	}

	public InspectCollectorConfiguration getConfiguration() {
		return configuration;
	}

	public void emitTrace(EventTrace trace) {
		if(nonNull(dispatcher)) {
			dispatcher.emit(trace);
		}
	}

	public void emitTask(DispatchTask task) {
		if(nonNull(dispatcher)) {
			dispatcher.emit(task);
		}
	}

	void traceStartupSession(Instant start) {
		var ses = createStartupSession(start);
		call(()->{
			ses.setName("main");
			ses.emit();
		});
		var call = ses.createCallback();
		sesCtx = call.setupContext(true);
	}

	void traceStartupSession(Instant instant, String className, String methodName, Throwable thrw) {
		if(nonNull(sesCtx)) {
			call(()->{
				var ses = (MainSessionCallback) sesCtx.callback;
				ses.setLocation(className, methodName);
				if(nonNull(thrw)) {  //nullable
					ses.setException(fromException(thrw));
				}
				ses.setEnd(instant);
				ses.emit();
			});
			sesCtx.release();
			sesCtx = null;
		}
		else {
			reportSessionIsNull("traceStartupSession");
		}
	}

	static void initializeInspectContext(Instant start, InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider) {
		var instance = contextInstance(start, conf, provider);
		var hooks = new ArrayList<DispatchHook>();
		if(conf.getMonitoring().getResources().isEnabled()) {
			hooks.add(new MachineResourceMonitor(conf.getMonitoring().getResources().getDisk()));
		}
		if(conf.isDebugMode()) {
			hooks.add(new EventTraceDebugger());
		}
		if(conf.getTracing().getDump().isEnabled()) {
			hooks.add(new EventTraceDumper(createDirs(conf.getTracing().getDump().getLocation(), instance.getId()), mapper));
		}
		DispatcherAgent agnt = null;
		if(conf.getTracing().getRemote() instanceof RestRemoteServerProperties prop) {
			agnt = new RestDispatcherAgent(prop, mapper);
		}
		else if(isNull(conf.getTracing().getRemote())) {
			agnt = noAgent(); //no remote agent
			log.warn("remote tracing is disabled, traces will be lost");
		}
		else {
			throw new UnsupportedOperationException("unsupported remote " + conf.getTracing().getRemote());
		}
		var dspt = new EventTraceScheduledDispatcher(conf.getTracing(), conf.getScheduling(), agnt, hooks);
		singleton = new InspectContext(conf, dspt);
		dspt.dispatch(instance);
	}

	static InstanceEnvironment contextInstance(Instant start, InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider) {
		return new InstanceEnvironment(nextId(),
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
		return json()
				.modules(new JavaTimeModule(), coreModule())
				.build()
				.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		//		.disable(WRITE_DATES_AS_TIMESTAMPS) important! write Instant as double
		//		.configure(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL, true) // force deserialize NamedType if @type is missing
	}

	public static SimpleModule coreModule() {
		return new SimpleModule("inspect-core-module").registerSubtypes(
				new NamedType(LogEntry.class, 					"log"),  
				new NamedType(MachineResourceUsage.class, 		"rsrc-usg"),
				new NamedType(MainSession.class,  				"main-ses"), 
				new NamedType(RestSession.class,  				"rest-ses"), 
				new NamedType(LocalRequest.class, 				"locl-req"), 
				new NamedType(DatabaseRequest.class,			"jdbc-req"),
				new NamedType(RestRequest.class,  				"http-req"), 
				new NamedType(MailRequest.class,  				"mail-req"), 
				new NamedType(DirectoryRequest.class,			"ldap-req"), 
				new NamedType(FtpRequest.class,  				"ftp-req"),
				new NamedType(DatabaseRequestStage.class,		"jdbc-stg"),
				new NamedType(HttpRequestStage.class,  			"http-stg"), 
				new NamedType(HttpSessionStage.class,  			"sess-stg"), 
				new NamedType(MailRequestStage.class,  			"mail-stg"), 
				new NamedType(DirectoryRequestStage.class,		"ldap-stg"), 
				new NamedType(FtpRequestStage.class,  			"ftp-stg"),
				new NamedType(RestRemoteServerProperties.class, "rest-rmt"));
	}

	static InspectCollectorConfiguration disabledConfiguration() {
		var conf = new InspectCollectorConfiguration();
		conf.setEnabled(false);
		conf.setDebugMode(false);
		conf.getScheduling().setState(DISABLE);
		conf.getTracing().setRemote(null); //avoid remote dispatching
		conf.getMonitoring().getResources().setEnabled(false); //avoid resource monitoring
		conf.getMonitoring().getException().setMaxStackTraceRows(0); //avoid memory leak
		conf.getMonitoring().getException().setMaxCauseDepth(0); //avoid memory leak
		return conf;
	}
}
