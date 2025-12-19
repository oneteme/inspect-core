package org.usf.inspect.dir;

import static java.net.URI.create;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.DirAction.CONNECTION;
import static org.usf.inspect.core.DirAction.DISCONNECTION;
import static org.usf.inspect.core.DirAction.EXECUTE;
import static org.usf.inspect.core.Monitor.traceBegin;
import static org.usf.inspect.core.Monitor.traceEnd;
import static org.usf.inspect.core.Monitor.traceStep;

import java.util.function.Function;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.usf.inspect.core.DirAction;
import org.usf.inspect.core.DirCommand;
import org.usf.inspect.core.DirectoryRequest2;
import org.usf.inspect.core.DirectoryRequestCallback;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Monitor;
import org.usf.inspect.core.SessionContextManager;

/**
 * 
 * @author u$f
 *
 */
final class DirectoryRequestMonitor implements Monitor {

	private DirectoryRequestCallback callback;
	
	ExecutionListener<DirContext> handleConnection() {
		ExecutionListener<DirContext> lstn = traceBegin(SessionContextManager::createNamingRequest, this::createCallback, (req,dir)->{
			if(nonNull(dir)) {
				var url = getEnvironmentVariable(dir, "java.naming.provider.url", v-> create(v.toString()));  //broke context dependency
				if(nonNull(url)) {
					req.setProtocol(url.getScheme());
					req.setHost(url.getHost());
					req.setPort(url.getPort());
				}
				req.setUser(getEnvironmentVariable(dir, "java.naming.security.principal", Object::toString));  //broke context dependency
			}
		});
		return lstn.then(stageHandler(CONNECTION, null)); //before end if thrw
	}
	
	//callback should be created before processing
	DirectoryRequestCallback createCallback(DirectoryRequest2 session) { 
		return callback = session.createCallback();
	}

	ExecutionListener<Void> handleDisconnection() {
		ExecutionListener<Void> lstn = stageHandler(DISCONNECTION, null);
		return lstn.then(traceEnd(callback));
	}
	
	<T> ExecutionListener<T> executeStageHandler(DirCommand cmd, String... args) {
		return stageHandler(EXECUTE, cmd, args);
	}
	
	<T> ExecutionListener<T> stageHandler(DirAction action, DirCommand cmd, String... args) {
		return traceStep(callback, (s,e,o,t)-> callback.createStage(action, s, e, t, cmd, args));
	}

	static <T> T getEnvironmentVariable(DirContext o, String key, Function<Object, T> fn) throws NamingException {
		var env = o.getEnvironment();
		if(nonNull(env) && env.containsKey(key)) {
			return fn.apply(env.get(key));
		}
		return null;
	}
}
