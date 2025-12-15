package org.usf.inspect.dir;

import static java.net.URI.create;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.DirAction.CONNECTION;
import static org.usf.inspect.core.DirAction.DISCONNECTION;
import static org.usf.inspect.core.DirAction.EXECUTE;
import static org.usf.inspect.core.Monitor.connectionHandler;
import static org.usf.inspect.core.Monitor.disconnectionHandler;
import static org.usf.inspect.core.Monitor.connectionStageHandler;

import java.util.function.Function;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.usf.inspect.core.DirCommand;
import org.usf.inspect.core.DirectoryRequest2;
import org.usf.inspect.core.DirectoryRequestCallback;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.Monitor;
import org.usf.inspect.core.SessionContextManager;

/**
 * 
 * @author u$f
 *
 */
final class DirectoryRequestMonitor implements Monitor {

	private DirectoryRequestCallback callback;
	
	public ExecutionHandler<DirContext> handleConnection() {
		return connectionHandler(SessionContextManager::createNamingRequest, this::createCallback, (req,dir)->{
			if(nonNull(dir)) {
				var url = getEnvironmentVariable(dir, "java.naming.provider.url", v-> create(v.toString()));  //broke context dependency
				if(nonNull(url)) {
					req.setProtocol(url.getScheme());
					req.setHost(url.getHost());
					req.setPort(url.getPort());
				}
				req.setUser(getEnvironmentVariable(dir, "java.naming.security.principal", Object::toString));  //broke context dependency
			}
		}, (req,s,e,o,t)-> req.createStage(CONNECTION, s, e, t, null)); //before end if thrw
	}
	
	//callback should be created before processing
	DirectoryRequestCallback createCallback(DirectoryRequest2 session) { 
		return callback = session.createCallback();
	}

	public ExecutionHandler<Void> handleDisconnection() {
		return disconnectionHandler(callback, (req,s,e,o,t)-> req.createStage(DISCONNECTION, s, e, t, null));
	}
	
	<T> ExecutionHandler<T> executeStageHandler(DirCommand cmd, String... args) {
		return connectionStageHandler(callback, (req,s,e,o,t)-> req.createStage(EXECUTE, s, e, t, cmd, args));
	}

	static <T> T getEnvironmentVariable(DirContext o, String key, Function<Object, T> fn) throws NamingException {
		var env = o.getEnvironment();
		if(nonNull(env) && env.containsKey(key)) {
			return fn.apply(env.get(key));
		}
		return null;
	}
}
