package org.usf.inspect.core;

import static java.lang.System.getProperty;
import static java.net.InetAddress.getLocalHost;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.runSafely;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.InstanceType.SERVER;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.createStartupSession;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;

import java.net.UnknownHostException;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class StartupMonitor implements Monitor {
	
	private MainSessionCallback call;

	public void beforeStartup(Instant start, ApplicationPropertiesProvider provider)  {
		var instance = newInstanceEnvironment(start, context().getConfiguration(), provider);
		context().dispatch(instance);
		call = createStartupSession(start, instance.getId(), ses-> ses.setName("main"));
		setActiveContext(call);
	}
	
	public void afterStartup(Instant end, Class<?> clazz, String method, Throwable thrw)  {
		if(assertStillOpened(call)) {
			runSafely(()->{
				call.setLocation(clazz.getName(), method);
				if(nonNull(thrw)) {  //nullable
					call.setException(fromException(thrw));
				}
				call.setEnd(end);
				emit(call);
			});
			clearContext(call);
			call = null;
		}
	}
	
	static InstanceEnvironment newInstanceEnvironment(Instant start, InspectCollectorConfiguration conf, ApplicationPropertiesProvider provider) {
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
}
