package org.usf.inspect.dir;

import static java.net.URI.create;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.DirAction.CONNECTION;
import static org.usf.inspect.core.DirAction.DISCONNECTION;
import static org.usf.inspect.core.DirAction.EXECUTE;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.createNamingRequest;

import java.time.Instant;
import java.util.function.Function;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.usf.inspect.core.DirCommand;
import org.usf.inspect.core.DirectoryRequest;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class DirectoryRequestMonitor {

	private final DirectoryRequest req = createNamingRequest();
	
	public DirectoryRequest handleConnection(Instant start, Instant end, DirContext dir, Throwable thw) throws NamingException {
		req.createStage(CONNECTION, start, end, null, thw).emit();
		req.setThreadName(threadName());
		req.setStart(start);
		if(nonNull(thw)) { //if connection error
			req.setEnd(end);
		}
		var url = getEnvironmentVariable(dir, "java.naming.provider.url", v-> create(v.toString()));  //broke context dependency
		if(nonNull(url)) {
			req.setProtocol(url.getScheme());
			req.setHost(url.getHost());
			req.setPort(url.getPort());
		}
		var user = getEnvironmentVariable(dir, "java.naming.security.principal", Object::toString); //broke context dependency
		if(nonNull(user)) {
			req.setUser(user);
		}
		return req;
	}

	public DirectoryRequest handleDisconnection(Instant start, Instant end, Void v, Throwable thw) {
		req.createStage(DISCONNECTION, start, end, null, thw).emit();
		req.runSynchronized(()-> req.setEnd(end));
		return req;
	}
	
	<T> ExecutionHandler<T> executeStageHandler(DirCommand cmd, String... args) {
		return (s,e,o,t)-> req.createStage(EXECUTE, s, e, cmd, t, args);
	}

	static <T> T getEnvironmentVariable(DirContext o, String key, Function<Object, T> fn) throws NamingException {
		var env = o.getEnvironment();
		if(nonNull(env) && env.containsKey(key)) {
			return fn.apply(env.get(key));
		}
		return null;
	}
}
