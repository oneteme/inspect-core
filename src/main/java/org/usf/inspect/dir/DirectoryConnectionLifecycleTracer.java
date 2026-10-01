package org.usf.inspect.dir;

import static java.net.URI.create;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.DirAction.CONNECTION;
import static org.usf.inspect.core.DirAction.DISCONNECTION;
import static org.usf.inspect.core.DirAction.EXECUTE;
import static org.usf.inspect.core.SessionContextManager.createNamingSignal;

import java.time.Instant;

import javax.naming.directory.DirContext;

import org.usf.inspect.core.ConnectionLifecycleTracer;
import org.usf.inspect.core.DirAction;
import org.usf.inspect.core.DirCommand;
import org.usf.inspect.core.DirectoryRequestSignal;
import org.usf.inspect.core.DirectoryRequestStage;
import org.usf.inspect.core.DirectoryRequestUpdate;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.StagePayload;
import org.usf.inspect.core.TraceSignal;

import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor
final class DirectoryConnectionLifecycleTracer extends ConnectionLifecycleTracer {

	@Override
	protected DirectoryRequestSignal signal(Instant start) {
		return createNamingSignal(start);
	}

	@Override
	protected DirectoryRequestUpdate update(TraceSignal signal) { 
		return new DirectoryRequestUpdate(signal.getId());
	}

	@Override
	public short resolveStatus(Throwable t) {
	    return switch (t) {
	    	case javax.naming.AuthenticationException e -> APP_UNAUTHORIZED;
	    	case javax.naming.AuthenticationNotSupportedException e-> APP_UNAUTHORIZED;
	    	case javax.naming.NameNotFoundException e -> APP_ERROR;
	    	case javax.naming.InvalidNameException e-> APP_ERROR;
        
	        case javax.naming.ServiceUnavailableException e -> CNX_REFUSED;
	        case javax.naming.CommunicationException e -> CNX_INTERRUPTED;
	        case javax.naming.InterruptedNamingException e -> CNX_INTERRUPTED;
	        
	        case javax.naming.TimeLimitExceededException e-> CNX_TIMEOUT;
	        
	        case javax.naming.NamingException e -> INT_ERROR;

			default -> super.resolveStatus(t);
	    };
	}
	
	public ExecutionListener<DirContext> connectionListener() {
		return connectionListener(stageBuilder(CONNECTION, null), (trc, cnx)->{
			var sgn = (DirectoryRequestSignal) trc;
			if(nonNull(cnx) && nonNull(cnx.getEnvironment())) {
				var val = cnx.getEnvironment().get("java.naming.provider.url");
				if(nonNull(val)) {
					var url = create(val.toString());
					sgn.setProtocol(url.getScheme());
					sgn.setHost(url.getHost());
					sgn.setPort(url.getPort());
				}
				val = cnx.getEnvironment().get("java.naming.security.principal");
				if(nonNull(val)) {
					sgn.setUser(val.toString());
				}
			}
		}); //before end if thrw
	}
	
	public ExecutionListener<Void> disconnectionListener() {
		return disconnectionListener(stageBuilder(DISCONNECTION, null));
	}
	
	public <T> ExecutionListener<T> stageHandler(DirCommand cmd, String... args) {
		return stageHandler(EXECUTE, cmd, args);
	}
	
	<T> ExecutionListener<T> stageHandler(DirAction action, DirCommand cmd, String... args) {
		return stageListener(stageBuilder(action, cmd, args));
	}
	
	<R> StageBuilder<R> stageBuilder(DirAction action, DirCommand cmd, String... args) {
		return (s,e,o,t)-> {
			var upd = (DirectoryRequestUpdate) getUpdate();
			var stg = new DirectoryRequestStage(upd.getId(), getStageCounter().incrementAndGet());
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			if(nonNull(cmd)) {
				stg.setCommand(cmd.name());
				upd.setCommand(merge(upd.getCommand(), cmd.getType())); //update request
			}
			stg.setPayload(nonNull(args) && args.length > 0 ? new StagePayload(args, null) : null);
			return stg;
		};
	}
}
