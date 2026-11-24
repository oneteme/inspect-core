package org.usf.inspect.sql;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.CommandType.merge;
import static org.usf.inspect.DatabaseAction.CONNECTION;
import static org.usf.inspect.DatabaseAction.EXECUTE;
import static org.usf.inspect.DatabaseAction.FETCH;
import static org.usf.inspect.core.SessionContextManager.createDatabaseSignal;
import static org.usf.inspect.core.TraceHub.hub;

import java.sql.Connection;
import java.time.Instant;

import org.usf.inspect.DatabaseAction;
import org.usf.inspect.DatabaseCommand;
import org.usf.inspect.DatabaseRequestSignal;
import org.usf.inspect.DatabaseRequestStage;
import org.usf.inspect.DatabaseRequestUpdate;
import org.usf.inspect.StagePayload;
import org.usf.inspect.TraceSignal;
import org.usf.inspect.core.ConnectionLifecycleTracer;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class DatabaseConnectionLifecycleTracer extends ConnectionLifecycleTracer {
	
	private final MetadataProvider metadataProvider;

	@Override
	protected DatabaseRequestSignal signal(Instant start) {
		return createDatabaseSignal(start);
	}

	@Override
	protected DatabaseRequestUpdate update(TraceSignal signal) { 
		return new DatabaseRequestUpdate(signal.getId());
	}
	
	@Override
	public short resolveStatus(Throwable t) {
	    return switch (t) {
	        case java.sql.SQLTransientConnectionException e -> CNX_ERROR;
	        case java.sql.SQLNonTransientConnectionException e -> CNX_REFUSED;
	        case java.sql.SQLRecoverableException e -> CNX_INTERRUPTED;

	        case java.sql.SQLTimeoutException e -> INT_TIMEOUT;

	        case java.sql.SQLSyntaxErrorException e -> APP_ERROR;
	        case java.sql.SQLInvalidAuthorizationSpecException e -> APP_UNAUTHORIZED;
	        case java.sql.SQLIntegrityConstraintViolationException e -> APP_CONFLICT;

	        case java.sql.SQLException e -> INT_ERROR;

	        default -> super.resolveStatus(t);
	    };
	}

	public ExecutionListener<Connection> connectionListener() {
		return connectionListener(stageBuilder(CONNECTION, null), (trc,cnx)->{
			var cache = metadataProvider.getMetadata(cnx);
			if(nonNull(cache)) {
				var sgn = (DatabaseRequestSignal) trc;
				sgn.setProtocol(cache.getScheme());
				sgn.setHost(cache.getHost());
				sgn.setPort(cache.getPort());
				sgn.setName(cache.getName()); //getCatalog
				sgn.setSchema(cache.getSchema());
				sgn.setUser(cache.getUser());
				sgn.setProductName(cache.getProductName());
				sgn.setProductVersion(cache.getProductVersion());
				sgn.setDriverVersion(cache.getDriverVersion());
			}
		});
	}
	
	public ExecutionListener<Object> disconnectionListener(DatabaseAction action) {
		return disconnectionListener(stageBuilder(action, null));
	}
	
	public StatementLifecycleTracer newStatementTracer(boolean prepared) {
		return new StatementLifecycleTracer(this, prepared);
	}
	
	public void emitFetchStage(Instant start, Instant end, long n) {
		hub().emitTrace(createStage(start, end, FETCH, null, new long[] {n})); //differed start
	}
	
	public <T> ExecutionListener<T> executeStageListener(DatabaseCommand cmd, String... args) {
		return stageListener(stageBuilder(EXECUTE, cmd, args));
	}
	
	public <T> ExecutionListener<T> stageListener(DatabaseAction action, DatabaseCommand cmd, String... args) {
		return stageListener(stageBuilder(action, cmd, args));
	}

	<R> StageBuilder<R> stageBuilder(DatabaseAction action, DatabaseCommand cmd, String... args) {
		return (s,e,o,t)-> createStage(s, e, action, cmd, null, args);
	}
	
	DatabaseRequestStage createStage(Instant start, Instant end, DatabaseAction action, DatabaseCommand cmd, long[] counts, String... args) {
		var upd = (DatabaseRequestUpdate)getUpdate();
		var stg = new DatabaseRequestStage(upd.getId(), getStageCounter().incrementAndGet());
		stg.setName(action.name());
		stg.setStart(start);
		stg.setEnd(end);
		if(nonNull(cmd)) {
			stg.setCommand(cmd.name());
			upd.setCommand(merge(upd.getCommand(), cmd.getType())); //update request
		}
		if(!isEmpty(args) || !isEmpty(counts)) {
			stg.setPayload(new StagePayload(args, counts));
		}
		return stg;
	}
	
	static <T> boolean isEmpty(T[] arr) {
		return isNull(arr) || arr.length == 0;
	}
	
	static boolean isEmpty(long[] arr) {
		return isNull(arr) || arr.length == 0;
	}
}
