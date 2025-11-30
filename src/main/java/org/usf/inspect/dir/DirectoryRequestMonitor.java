package org.usf.inspect.dir;

import static java.net.URI.create;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Callback.assertStillOpened;
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

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class DirectoryRequestMonitor {

	private DirectoryRequestCallback callback;
	
	public void handleConnection(Instant start, Instant end, DirContext dir, Throwable thw) throws NamingException {
		var req = createNamingRequest(start);
		if(nonNull(dir)) {
			var url = getEnvironmentVariable(dir, "java.naming.provider.url", v-> create(v.toString()));  //broke context dependency
			if(nonNull(url)) {
				req.setProtocol(url.getScheme());
				req.setHost(url.getHost());
				req.setPort(url.getPort());
			}
			req.setUser(getEnvironmentVariable(dir, "java.naming.security.principal", Object::toString));  //broke context dependency
		}
		req.emit();
		callback = req.createCallback();
		callback.createStage(CONNECTION, start, end, thw, null).emit(); //before end if thrw
		if(nonNull(thw)) { //if connection error
			callback.setEnd(end);
			callback.emit();
			callback = null;
		}
	}

	public void handleDisconnection(Instant start, Instant end, Void v, Throwable thw) {
		if(assertStillOpened(callback)) { // report if request was closed
			callback.createStage(DISCONNECTION, start, end, thw, null).emit();
			callback.setEnd(end);
			callback.emit(); //avoid emit twice
			callback = null;
		}
	}
	
	<T> ExecutionHandler<T> executeStageHandler(DirCommand cmd, String... args) {
		return (s,e,o,t)-> {
			if(assertStillOpened(callback)) { // report if request was closed
				callback.createStage(EXECUTE, s, e, t, cmd, args).emit();
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
