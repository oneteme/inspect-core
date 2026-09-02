package org.usf.inspect.dir;

import static java.net.URI.create;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.DirAction.CONNECTION;
import static org.usf.inspect.core.DirAction.DISCONNECTION;
import static org.usf.inspect.core.DirAction.EXECUTE;
import static org.usf.inspect.core.SessionContextManager.createNamingSignal;

import java.time.Instant;
import java.util.function.Function;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.usf.inspect.core.DirAction;
import org.usf.inspect.core.DirCommand;
import org.usf.inspect.core.DirectoryRequestSignal;
import org.usf.inspect.core.DirectoryRequestStage;
import org.usf.inspect.core.DirectoryRequestUpdate;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Monitor.StageBuilder;
import org.usf.inspect.core.StagePayload;
import org.usf.inspect.core.StatefulExecutionListener;
import org.usf.inspect.core.TraceSignal;

import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor
final class DirectoryRequestListener extends StatefulExecutionListener<DirContext> {

	@Override
	protected DirectoryRequestSignal signal(Instant start, DirContext cnx) throws NamingException {
		var sgn = createNamingSignal(start);
		if(nonNull(cnx)) {
			var url = getEnvironmentVariable(cnx, "java.naming.provider.url", v-> create(v.toString()));  //broke context dependency
			if(nonNull(url)) {
				sgn.setProtocol(url.getScheme());
				sgn.setHost(url.getHost());
				sgn.setPort(url.getPort());
			}
			sgn.setUser(getEnvironmentVariable(cnx, "java.naming.security.principal", Object::toString));  //broke context dependency
		}
		return sgn;
	}

	@Override
	protected DirectoryRequestUpdate update(TraceSignal signal) { 
		return new DirectoryRequestUpdate(signal.getId());
	}

	@Override
	protected int resolveStatus(Throwable t) {
	    return switch (t) {
	    	case javax.naming.AuthenticationException e -> CLIENT_UNAUTHORIZED;
	    	case javax.naming.NameNotFoundException e -> CLIENT_ERROR;
	    	case javax.naming.InvalidNameException e-> CLIENT_ERROR;
        
	        case javax.naming.ServiceUnavailableException e -> CONN_REFUSED;
	        case javax.naming.CommunicationException e -> CONN_INTERRUPTED;
	        case javax.naming.InterruptedNamingException e -> CONN_INTERRUPTED;
	        
	        case javax.naming.NamingException e -> SERVER_ERROR;

			default -> super.resolveStatus(t);
	    };
	}
	
	ExecutionListener<DirContext> connectionListener() {
		return connectionListener(stageBuilder(CONNECTION, null)); //before end if thrw
	}
	
	ExecutionListener<Void> disconnectionListener() {
		return disconnectionListener(stageBuilder(DISCONNECTION, null));
	}
	
	<T> ExecutionListener<T> stageHandler(DirCommand cmd, String... args) {
		return stageHandler(EXECUTE, cmd, args);
	}
	
	<T> ExecutionListener<T> stageHandler(DirAction action, DirCommand cmd, String... args) {
		if(nonNull(cmd) && nonNull(getTrace())) {
			var upd = (DirectoryRequestUpdate) getTrace();
			upd.setCommand(merge(upd.getCommand(), cmd.getType()));
		}
		return stageListener(stageBuilder(action, cmd, args));
	}
	
	<R> StageBuilder<R> stageBuilder(DirAction action, DirCommand cmd, String... args) {
		return (s,e,o,t)-> {
			var upd = getTrace();
			var stg = new DirectoryRequestStage(upd.getId(), getStageCounter().incrementAndGet());
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			if(nonNull(cmd)) {
				stg.setCommand(cmd.name());
			}
			stg.setPayload(nonNull(args) && args.length > 0 ? new StagePayload(args, null) : null);
			return stg;
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
