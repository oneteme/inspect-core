package org.usf.inspect.dir;

import static java.net.URI.create;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.DirAction.CONNECTION;
import static org.usf.inspect.core.DirAction.DISCONNECTION;
import static org.usf.inspect.core.DirAction.EXECUTE;
import static org.usf.inspect.core.SessionContextManager.createNamingRequest;

import java.time.Instant;
import java.util.function.Function;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.usf.inspect.core.DirCommand;
import org.usf.inspect.core.DirectoryRequestCallback;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.Monitor;

/**
 * 
 * @author u$f
 *
 */
final class DirectoryRequestMonitor implements Monitor {

	private DirectoryRequestCallback callback;
	
	public void handleConnection(Instant start, Instant end, DirContext dir, Throwable thw) {
		callback = createNamingRequest(start, req->{
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
		emit(callback.createStage(CONNECTION, start, end, thw, null)); //before end if thrw
		if(nonNull(thw)) { //if connection error
			callback.setEnd(end);
			emit(callback);
			callback = null;
		}
	}

	public void handleDisconnection(Instant start, Instant end, Void v, Throwable thw) {
		if(assertStillOpened(callback)) { //report if request was closed, avoid emit trace twice
			emit(callback.createStage(DISCONNECTION, start, end, thw, null));
			callback.setEnd(end);
			emit(callback);
			callback = null;
		}
	}
	
	<T> ExecutionHandler<T> executeStageHandler(DirCommand cmd, String... args) {
		return (s,e,o,t)-> {
			if(assertStillOpened(callback)) { // report if request was closed
				emit(callback.createStage(EXECUTE, s, e, t, cmd, args));
			}
		};
	}

	static <T> T getEnvironmentVariable(DirContext o, String key, Function<Object, T> fn) throws NamingException {
		var env = o.getEnvironment();
		if(nonNull(env) && env.containsKey(key)) {
			return fn.apply(env.get(key));
		}
		return null;
	}
}
