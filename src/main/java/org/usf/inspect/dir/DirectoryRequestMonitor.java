package org.usf.inspect.dir;

import static java.net.URI.create;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.DirAction.CONNECTION;
import static org.usf.inspect.core.DirAction.DISCONNECTION;
import static org.usf.inspect.core.DirAction.EXECUTE;
import static org.usf.inspect.core.ExceptionInfo.fromException2;
import static org.usf.inspect.core.ExceptionInfo.rootCauseException;
import static org.usf.inspect.core.RequestCommonStatus.CLIENT_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.CLIENT_UNAUTHORIZED;
import static org.usf.inspect.core.RequestCommonStatus.CONN_INTERRUPTED;
import static org.usf.inspect.core.RequestCommonStatus.CONN_REFUSED;
import static org.usf.inspect.core.RequestCommonStatus.SERVER_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.SUCCESS;
import static org.usf.inspect.core.RequestCommonStatus.statusFor;

import java.util.function.Function;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.usf.inspect.core.DirAction;
import org.usf.inspect.core.DirCommand;
import org.usf.inspect.core.DirectoryRequestSignal;
import org.usf.inspect.core.DirectoryRequestUpdate;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Monitor.StatefulMonitor;
import org.usf.inspect.core.SessionContextManager;

/**
 * 
 * @author u$f
 *
 */
final class DirectoryRequestMonitor extends StatefulMonitor<DirectoryRequestSignal, DirectoryRequestUpdate> {

	ExecutionListener<DirContext> handleConnection() {
		return traceBegin(SessionContextManager::createNamingRequest, (req,dir)->{
			if(nonNull(dir)) {
				var url = getEnvironmentVariable(dir, "java.naming.provider.url", v-> create(v.toString()));  //broke context dependency
				if(nonNull(url)) {
					req.setProtocol(url.getScheme());
					req.setHost(url.getHost());
					req.setPort(url.getPort());
				}
				req.setUser(getEnvironmentVariable(dir, "java.naming.security.principal", Object::toString));  //broke context dependency
			}
		}, stageHandler(CONNECTION, null)); //before end if thrw
	}
	
	//callback should be created before processing
	protected DirectoryRequestUpdate createCallback(DirectoryRequestSignal session) { 
		return session.createCallback();
	}

	ExecutionListener<Void> handleDisconnection() {
		return traceEnd(stageHandler(DISCONNECTION, null));
	}
	
	<T> ExecutionListener<T> executeStageHandler(DirCommand cmd, String... args) {
		return stageHandler(EXECUTE, cmd, args);
	}
	
	<T> ExecutionListener<T> stageHandler(DirAction action, DirCommand cmd, String... args) {
		return traceStep((s,e,o,t)-> {
			var upd = getCallback();
			var stg = upd.createStage();
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			if(nonNull(cmd)) {
				stg.setCommand(cmd.name());
				upd.setCommand(merge(upd.getCommand(), cmd.getType()));
			}
			if(nonNull(t)) {
				var root = rootCauseException(t);
				upd.setStatus(resolveStatus(root));
				stg.setException(fromException2(root));
			}
			else {
				upd.setStatus(SUCCESS);
			}
			stg.setArgs(args);
			return stg;
		});
	}

	static <T> T getEnvironmentVariable(DirContext o, String key, Function<Object, T> fn) throws NamingException {
		var env = o.getEnvironment();
		if(nonNull(env) && env.containsKey(key)) {
			return fn.apply(env.get(key));
		}
		return null;
	}

	static int resolveStatus(Throwable t) {
	    if (isNull(t)) {
	        return SUCCESS;
	    }
	    return switch (t) {
	    	case javax.naming.AuthenticationException e -> CLIENT_UNAUTHORIZED;
	    	case javax.naming.NameNotFoundException e -> CLIENT_ERROR;
        
	        case javax.naming.ServiceUnavailableException e -> CONN_REFUSED;
	        case javax.naming.CommunicationException e -> CONN_INTERRUPTED;
	        case javax.naming.InterruptedNamingException e -> CONN_INTERRUPTED;
	        
	        case javax.naming.NamingException e -> SERVER_ERROR;
	        
			default -> statusFor(t);
	    };
	}

}
