package org.usf.inspect.jdbc;

import static java.util.Arrays.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.DatabaseAction.BATCH;
import static org.usf.inspect.core.DatabaseAction.CONNECTION;
import static org.usf.inspect.core.DatabaseAction.DISCONNECTION;
import static org.usf.inspect.core.DatabaseAction.EXECUTE;
import static org.usf.inspect.core.DatabaseAction.FETCH;
import static org.usf.inspect.core.DatabaseAction.STATEMENT;
import static org.usf.inspect.core.DatabaseCommand.SQL;
import static org.usf.inspect.core.DatabaseCommand.extractCommand;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.Helper.rootCauseException;
import static org.usf.inspect.core.RequestCommonStatus.CLIENT_CONFLICT;
import static org.usf.inspect.core.RequestCommonStatus.CLIENT_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.CLIENT_UNAUTHORIZED;
import static org.usf.inspect.core.RequestCommonStatus.CONN_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.CONN_INTERRUPTED;
import static org.usf.inspect.core.RequestCommonStatus.CONN_REFUSED;
import static org.usf.inspect.core.RequestCommonStatus.SERVER_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.SERVER_TIMEOUT;
import static org.usf.inspect.core.RequestCommonStatus.SUCCESS;
import static org.usf.inspect.core.RequestCommonStatus.statusFor;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.usf.inspect.core.DatabaseAction;
import org.usf.inspect.core.DatabaseCommand;
import org.usf.inspect.core.DatabaseRequestSignal;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.DatabaseRequestUpdate;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Monitor.StatefulMonitor;
import org.usf.inspect.core.SessionContextManager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class DatabaseRequestMonitor extends StatefulMonitor<DatabaseRequestSignal, DatabaseRequestUpdate> {

	private final ConnectionMetadataCache cache; //required

	private boolean prepared;
	private DatabaseCommand mainCommand;
	private DatabaseRequestStage lastExec; // hold last stage
	
	BatchStageHandler batchHandler = null;
	
	public ExecutionListener<Connection> connectionHandler() {
		return traceBegin(SessionContextManager::createDatabaseRequest, (req,cnx)->{
			if(nonNull(cnx) && !cache.isPresent()) {
				cache.update(cnx.getMetaData());
			}
			if(cache.isPresent()) {
				req.setScheme(cache.getScheme());
				req.setHost(cache.getHost());
				req.setPort(cache.getPort());
				req.setName(cache.getName()); //getCatalog
				req.setSchema(cache.getSchema());
				req.setUser(cache.getUser());
				req.setProductName(cache.getProductName());
				req.setProductVersion(cache.getProductVersion());
				req.setDriverVersion(cache.getDriverVersion());
			}
		}, stageHandler(CONNECTION)); //before end if thrw
	}

	//callback should be created before processing
	@Override
	protected DatabaseRequestUpdate createCallback(DatabaseRequestSignal session) { 
		return session.createCallback();
	}
	
	public ExecutionListener<Object> statementStageHandler(String sql) {
		mainCommand = null; //rest
		if(nonNull(sql)) {
			prepared = true;
			parseAndMergeCommand(sql);
		}
		return stageHandler(STATEMENT);
	}

	public ExecutionListener<Void> addBatchStageHandler(String sql) {
		if(nonNull(sql)) {
			parseAndMergeCommand(sql);
		}
		return isNull(batchHandler) ? traceStep((s,e,v,t)-> {
			var stg = createStage(s, e, t, BATCH, null, new long[] {1});
			if(nonNull(t)) {
				return stg;
			}
			batchHandler = new BatchStageHandler(stg);
			return null;
		}) : batchHandler;
	}
	
	public ExecutionListener<ResultSet> executeQueryStageHandler(String sql) {
		return executeStageHandler(sql, rs-> null); // no count 
	}

	public ExecutionListener<Boolean> executeStageHandler(String sql) {
		return executeStageHandler(sql, b-> null); //-1 if select, cannot call  getUpdateCount
	}

	public ExecutionListener<Integer> executeUpdateStageHandler(String sql) {
		return executeStageHandler(sql, n-> new long[] {n});
	}

	public ExecutionListener<Long> executeLargeUpdateStageHandler(String sql) {
		return executeStageHandler(sql, n-> new long[] {n});
	}

	public ExecutionListener<int[]> executeBatchStageHandler(){
		emitBatchStage(); //before batch execute
		return executeStageHandler(null, arr-> {
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

	public ExecutionListener<long[]> executeLargeBatchStageHandler() {
		emitBatchStage(); //before batch execute
		return executeStageHandler(null, arr-> {
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
		if(nonNull(batchHandler)) { //batch & largeBatch
			hub().emitTrace(batchHandler.getStage());
			batchHandler = null;
		}
		else {
			hub().reportMessage(false, "emitBatchStage", "empty batch or already traced");
		}
	}

	private <T> ExecutionListener<T> executeStageHandler(String sql, Function<T, long[]> countFn) {
		if(nonNull(sql)) { //statement
			parseAndMergeCommand(sql); //command set on exec stg
		}
		return traceStep((s,e,o,t)-> {
			lastExec = createStage(s, e, t, EXECUTE, mainCommand, nonNull(o) ? countFn.apply(o) : null); // o may be null, if execution failed
			if(!prepared) { //else multiple preparedStmt execution
				mainCommand = null;
			}
			return lastExec; //wait for rows count update before submit
		});
	}

	public void updateStageRowsCount(long rows) {
		if(rows > -1) {
			try { //lastStg may be already sent !!
				if(nonNull(lastExec) && EXECUTE.name().equals(lastExec.getName())) {
					var arr = lastExec.getCount();
					lastExec.setCount(isNull(arr) ? new long[] {rows} : appendLong(arr, rows)); // getMoreResults
				}
			}
			catch (Exception e) {
				hub().reportError(false, "DatabaseRequestMonitor.updateStageRowsCount", e);
			}
		}
	}

	public <T> ExecutionListener<T> fetch(Instant start, int n) {
		return traceStep((s,e,o,t)-> createStage(start, e, t, FETCH, null, new long[] {n})); //differed start
	}
	
	public ExecutionListener<Object> disconnectionHandler() {
		return traceEnd(stageHandler(DISCONNECTION));
	}

	<T> ExecutionListener<T> stageHandler(DatabaseAction action, String... args) {
		return stageHandler(action, null, args);
	}

	<T> ExecutionListener<T> stageHandler(DatabaseAction action, DatabaseCommand cmd, String... args) {
		return traceStep((s,e,o,t)-> createStage(s, e, t, action, cmd, args));
	}
	
	 DatabaseRequestStage createStage(Instant start, Instant end, Throwable thrw, DatabaseAction action, DatabaseCommand cmd, long[] count) {
		var stg = createStage(start, end, thrw, action, cmd);
		stg.setCount(count);
		return stg;
	}
	
	DatabaseRequestStage createStage(Instant start, Instant end, Throwable thrw, DatabaseAction action, DatabaseCommand cmd, String... args) {
		var upd = getCallback();
		var stg = upd.createStage();
		stg.setName(action.name());
		stg.setStart(start);
		stg.setEnd(end);
		if(nonNull(cmd)) {
			stg.setCommand(cmd.name());
			upd.setCommand(merge(upd.getCommand(), cmd.getType()));
		}
		if(nonNull(thrw)) {
			var root = rootCauseException(thrw);
			stg.setException(fromException(root, 0, 0)); //no stack trace
			if(upd.getStatus() < 0 ||  upd.getStatus() == SUCCESS) { //if success or no error, set status
				upd.setStatus(resolveStatus(root));
			}
		}
		else {
			upd.setStatus(SUCCESS);
		}
		stg.setArgs(args);
		return stg;
	}
	
	
	static long[] appendLong(long[]arr, long v) {
		var a = copyOf(arr, arr.length+1);
		a[arr.length] = v;
		return a;
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
	final class BatchStageHandler implements ExecutionListener<Void> {

		private final DatabaseRequestStage stage;
		
		public BatchStageHandler(DatabaseRequestStage stage) {
			this.stage = stage;
		}

		@Override
		public void handle(Instant start, Instant end, Void o, Throwable thrw) {
			stage.getCount()[0]++;
			stage.setEnd(end); //optim this		
			if(nonNull(thrw)) {
				batchHandler = null; //reset batching trace
				var root = rootCauseException(thrw);
				stage.setException(fromException(root, 0, 0)); //no stack trace
				var upd = getCallback();
				if(upd.getStatus() < 0 ||  upd.getStatus() == SUCCESS) { //if success or no error, set status
					upd.setStatus(resolveStatus(root));
				}
				hub().emitTrace(stage);
			}
		}
	}

	static int resolveStatus(Throwable t) {
	    if (isNull(t)) {
	        return SUCCESS;
	    }
	    return switch (t) {

	        case java.sql.SQLTransientConnectionException e -> CONN_ERROR;
	        case java.sql.SQLNonTransientConnectionException e -> CONN_REFUSED;
	        case java.sql.SQLRecoverableException e -> CONN_INTERRUPTED;

	        case java.sql.SQLTimeoutException e -> SERVER_TIMEOUT;

	        case java.sql.SQLSyntaxErrorException e -> CLIENT_ERROR;
	        case java.sql.SQLInvalidAuthorizationSpecException e -> CLIENT_UNAUTHORIZED;
	        case java.sql.SQLIntegrityConstraintViolationException e -> CLIENT_CONFLICT;

	        case java.sql.SQLException e -> SERVER_ERROR;

	        default -> statusFor(t);
	    };
	}
}
