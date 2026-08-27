package org.usf.inspect.jdbc;

import static java.util.Arrays.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.DatabaseAction.BATCH;
import static org.usf.inspect.core.DatabaseAction.CONNECTION;
import static org.usf.inspect.core.DatabaseAction.DISCONNECTION;
import static org.usf.inspect.core.DatabaseAction.EXECUTE;
import static org.usf.inspect.core.DatabaseAction.FETCH;
import static org.usf.inspect.core.DatabaseAction.STATEMENT;
import static org.usf.inspect.core.DatabaseCommand.SQL;
import static org.usf.inspect.core.DatabaseCommand.extractCommand;
import static org.usf.inspect.core.SessionContextManager.createDatabaseSignal;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.usf.inspect.core.DatabaseAction;
import org.usf.inspect.core.DatabaseCommand;
import org.usf.inspect.core.DatabaseRequestSignal;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.DatabaseRequestUpdate;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Monitor.StageBuilder;
import org.usf.inspect.core.StagePayload;
import org.usf.inspect.core.StatefulExecutionListener;
import org.usf.inspect.core.TraceSignal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class DatabaseRequestListener extends StatefulExecutionListener<Connection> {

	private final ConnectionMetadataCache cache; //required

	private boolean prepared;
	private DatabaseCommand mainCommand;
	private DatabaseRequestStage lastExec; // hold last stage
	
	BatchStageBuilder batchStageBuilder;
	
	@Override
	protected DatabaseRequestSignal signal(Instant start, Connection cnx) throws SQLException {
		var sgn = createDatabaseSignal(start);
		if(nonNull(cnx) && !cache.isPresent()) {
			cache.update(cnx.getMetaData());
		}
		if(cache.isPresent()) {
			sgn.setScheme(cache.getScheme());
			sgn.setHost(cache.getHost());
			sgn.setPort(cache.getPort());
			sgn.setName(cache.getName()); //getCatalog
			sgn.setSchema(cache.getSchema());
			sgn.setUser(cache.getUser());
			sgn.setProductName(cache.getProductName());
			sgn.setProductVersion(cache.getProductVersion());
			sgn.setDriverVersion(cache.getDriverVersion());
		}
		return sgn;
	}

	@Override
	protected DatabaseRequestUpdate update(TraceSignal signal) { 
		return new DatabaseRequestUpdate(signal.getId());
	}
	
	@Override
	protected int resolveStatus(Throwable t) {
	    return switch (t) {
	        case java.sql.SQLTransientConnectionException e -> CONN_ERROR;
	        case java.sql.SQLNonTransientConnectionException e -> CONN_REFUSED;
	        case java.sql.SQLRecoverableException e -> CONN_INTERRUPTED;

	        case java.sql.SQLTimeoutException e -> SERVER_TIMEOUT;

	        case java.sql.SQLSyntaxErrorException e -> CLIENT_ERROR;
	        case java.sql.SQLInvalidAuthorizationSpecException e -> CLIENT_UNAUTHORIZED;
	        case java.sql.SQLIntegrityConstraintViolationException e -> CLIENT_CONFLICT;

	        case java.sql.SQLException e -> SERVER_ERROR;

	        default -> super.resolveStatus(t);
	    };
	}
	
	public ExecutionListener<Connection> connectionListener() {
		return connectionListener(stageBuilder(CONNECTION, null));
	}
	
	public ExecutionListener<Object> disconnectionListener() {
		return disconnectionListener(stageBuilder(DISCONNECTION, null));
	}

	public ExecutionListener<Object> statementStageListener(String sql) {
		mainCommand = null; //rest
		if(nonNull(sql)) {
			prepared = true;
			parseAndMergeCommand(sql);
		}
		return stageListener(STATEMENT);
	}

	public ExecutionListener<Void> addBatchStageListener(String sql) {
		if(nonNull(sql)) {
			parseAndMergeCommand(sql);
		}
		if(isNull(batchStageBuilder)) {
			batchStageBuilder = new BatchStageBuilder(); 
		}
		return stageListener(batchStageBuilder);
	}
	
	public ExecutionListener<ResultSet> executeQueryStageListener(String sql) {
		return executeStageListener(sql, rs-> null); // no count 
	}

	public ExecutionListener<Boolean> executeStageListener(String sql) {
		return executeStageListener(sql, b-> null); //-1 if select, cannot call  getUpdateCount
	}

	public ExecutionListener<Integer> executeUpdateStageListener(String sql) {
		return executeStageListener(sql, n-> new long[] {n});
	}

	public ExecutionListener<Long> executeLargeUpdateStageListener(String sql) {
		return executeStageListener(sql, n-> new long[] {n});
	}

	public ExecutionListener<int[]> executeBatchStageListener(){
		emitBatchStage(); //before batch execute
		return executeStageListener(null, arr-> {
			if(arr.length > 1) { 
				var i=0;
				while(++i<arr.length && arr[i]==arr[0]);
				if(i==arr.length){
					return new long[] {i*arr[0]}; // [n,n,n,..,n] => [nN]
				}
			}
			return IntStream.of(arr).mapToLong(v->v).toArray();
		});
	}

	public ExecutionListener<long[]> executeLargeBatchStageListener() {
		emitBatchStage(); //before batch execute
		return executeStageListener(null, arr-> {
			if(arr.length > 1) {
				var i=0;
				while(++i<arr.length && arr[i]==arr[0]);
				if(i==arr.length){
					return new long[] {i*arr[0]}; // [n,n,n,..,n] => [nN]
				}
			}
			return arr;
		});
	}
	
	void emitBatchStage() { //wait for last addBatch
		if(nonNull(batchStageBuilder)) { //batch & largeBatch
			hub().emitTrace(batchStageBuilder.getStage());
			batchStageBuilder = null;
		}
		else {
			hub().reportMessage(false, "emitBatchStage", "empty batch or already traced");
		}
	}

	private <T> ExecutionListener<T> executeStageListener(String sql, Function<T, long[]> countFn) {
		if(nonNull(sql)) { //statement
			parseAndMergeCommand(sql); //command set on exec stg
		}
		if(!prepared) { //else multiple preparedStmt execution
			mainCommand = null;
		}
		return stageListener((s,e,o,t)-> 
			lastExec = createStage(s, e, EXECUTE, mainCommand, nonNull(o) ? countFn.apply(o) : null)); // o may be null, if execution failed
	}

	public void updateStageRowsCount(long rows) {
		if(rows > -1) {
			try { //lastStg may be already sent !!
				if(nonNull(lastExec) && EXECUTE.name().equals(lastExec.getName())) {
					var payload = lastExec.getPayload();
					if(isNull(payload)) {
						payload = new StagePayload(null, new long[]{rows});
						lastExec.setPayload(payload);
					}
					else {
						payload.setCount(appendLong(payload.getCount(), rows)); // getMoreResults
					}
				}
			}
			catch (Exception e) {
				hub().reportError(false, "DatabaseRequestMonitor.updateStageRowsCount", e);
			}
		}
	}

	public <T> ExecutionListener<T> fetchStageListener(Instant start, int n) {
		return stageListener((s,e,o,t)-> createStage(start, e, FETCH, null, new long[] {n})); //differed start
	}
	
	public <T> ExecutionListener<T> executeStageListener(DatabaseCommand cmd, String... args) {
		return stageListener(stageBuilder(EXECUTE, cmd, args));
	}
	
	public <T> ExecutionListener<T> stageListener(DatabaseAction action, String... args) {
		return stageListener(stageBuilder(action, null, args));
	}

	<R> StageBuilder<R> stageBuilder(DatabaseAction action, DatabaseCommand cmd, String... args) {
		return (s,e,o,t)-> createStage(s, e, action, cmd, nonNull(args) ? new StagePayload(args, null) : null);
	}
	
	DatabaseRequestStage createStage(Instant start, Instant end, DatabaseAction action, DatabaseCommand cmd, long[] count) {
		return createStage(start, end, action, cmd, nonNull(count) ? new StagePayload(null, count) : null);
	}

	DatabaseRequestStage createStage(Instant start, Instant end, DatabaseAction action, DatabaseCommand cmd, StagePayload payload) {
		var upd = getTrace();
		var stg = new DatabaseRequestStage(upd.getId(), getStageCounter().incrementAndGet());
		stg.setName(action.name());
		stg.setStart(start);
		stg.setEnd(end);
		if(nonNull(cmd)) {
			stg.setCommand(cmd.name());
		}
		stg.setPayload(payload);
		return stg;
	}
	
	static long[] appendLong(long[]arr, long v) {
		if(nonNull(arr)) {
			var a = copyOf(arr, arr.length+1);
			a[arr.length] = v;
			return a;
		}
		return new long[] {v};
	}
	
	void parseAndMergeCommand(String sql) {
		try {
			mainCommand = mergeCommand(mainCommand, extractCommand(sql));
		}
		catch (Exception e) {
			hub().reportError(false, "parseAndMergeCommand", e);
		}
	}
	
	static DatabaseCommand mergeCommand(DatabaseCommand main, DatabaseCommand cmd) {
		if(main == cmd || isNull(cmd)) {
			return main;
		}
		return isNull(main) ? cmd : SQL;
	}
	
	@Getter
	final class BatchStageBuilder implements StageBuilder<Void> {

		private DatabaseRequestStage stage;
		
		@Override
		public DatabaseRequestStage newStage(Instant start, Instant end, Void obj, Throwable thrw) {
			if(isNull(stage)) {
				stage = createStage(start, end, BATCH, null, new long[] {1}); 
			}
			else {
				stage.getPayload().getCount()[0]++;
				stage.setEnd(end); //optim this	
			}	
			if(nonNull(thrw)) {
				batchStageBuilder = null; //reset batching trace
				return stage;
			}
			return null;
		}
	}
}
