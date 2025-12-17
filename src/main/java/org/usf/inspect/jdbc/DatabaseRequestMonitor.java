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
import static org.usf.inspect.core.DatabaseCommand.parseCommand;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.Monitor.traceBegin;
import static org.usf.inspect.core.Monitor.traceStep;
import static org.usf.inspect.core.Monitor.traceEnd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.usf.inspect.core.DatabaseAction;
import org.usf.inspect.core.DatabaseCommand;
import org.usf.inspect.core.DatabaseRequest2;
import org.usf.inspect.core.DatabaseRequestCallback;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.SessionContextManager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class DatabaseRequestMonitor {

	private final ConnectionMetadataCache cache; //required

	private DatabaseRequestCallback callback;
	private boolean prepared;
	private DatabaseCommand mainCommand;
	private DatabaseRequestStage lastStg; // hold last stage
	
	BatchStageHandler batchHandler = null;
	
	public ExecutionListener<Connection> handleConnection() {
		return traceBegin(SessionContextManager::createDatabaseRequest, this::createCallback, (req,cnx)->{
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
		}, (s,e,o,t)-> callback.createStage(CONNECTION, s, e, t, null)); //before end if thrw
	}

	//callback should be created before processing
	DatabaseRequestCallback createCallback(DatabaseRequest2 session) { 
		return callback = session.createCallback();
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
		return isNull(batchHandler) ? traceStep(callback, (s,e,v,t)-> {
			var stg = callback.createStage(BATCH, s, e, t, null, new long[] {1});
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
			context().emitTrace(batchHandler.getStage());
			batchHandler = null;
		}
		else {
			context().reportMessage(false, "emitBatchStage", "empty batch or already traced");
		}
	}

	private <T> ExecutionListener<T> executeStageHandler(String sql, Function<T, long[]> countFn) {
		if(nonNull(sql)) { //statement
			parseAndMergeCommand(sql); //command set on exec stg
		}
		return traceStep(callback, (s,e,o,t)-> {
			lastStg = callback.createStage(EXECUTE, s, e, t, mainCommand, nonNull(o) ? countFn.apply(o) : null); // o may be null, if execution failed
			if(!prepared) { //else multiple preparedStmt execution
				mainCommand = null;
			}
			return lastStg; //wait for rows count update before submit
		});
	}

	public void updateStageRowsCount(long rows) {
		if(rows > -1) {
			try { //lastStg may be already sent !!
				if(nonNull(lastStg) && EXECUTE.name().equals(lastStg.getName())) {
					var arr = lastStg.getCount();
					lastStg.setCount(isNull(arr) ? new long[] {rows} : appendLong(arr, rows)); // getMoreResults
				}
			}
			catch (Exception e) {
				context().reportError(false, "DatabaseRequestMonitor.updateStageRowsCount", e);
			}
		}
	}

	public <T> ExecutionListener<T> fetch(Instant start, int n) {
		return traceStep(callback, 
				(s,e,o,t)-> callback.createStage(FETCH, start, e, t, null, new long[] {n})); //differed start 
	}
	
	public ExecutionListener<Void> handleDisconnection() { //sonar: used as lambda
		return traceEnd(callback, (s,e,o,t)-> callback.createStage(DISCONNECTION, s, e, t, null));
	}

	public ExecutionListener<Object> stageHandler(DatabaseAction action, String... args) {
		return stageHandler(action, null, args);
	}

	public ExecutionListener<Object> stageHandler(DatabaseAction action, DatabaseCommand cmd, String... args) {
		return traceStep(callback, (s,e,o,t)-> callback.createStage(action, s, e, t, cmd, args));
	}
	
	static long[] appendLong(long[]arr, long v) {
		var a = copyOf(arr, arr.length+1);
		a[arr.length] = v;
		return a;
	}
	
	void parseAndMergeCommand(String sql) {
		try {
			mainCommand = mergeCommand(mainCommand, parseCommand(sql));
		}
		catch (Exception e) {
			context().reportError(false, "parseAndMergeCommand", e);
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
		public void handle(Instant start, Instant end, Void o, Throwable t) {
			stage.getCount()[0]++;
			stage.setEnd(end); //optim this		
			if(nonNull(t)) {
				batchHandler = null; //reset batching trace
				stage.setException(mainCauseException(t)); //may overwrite previous
				context().emitTrace(stage);
			}
		}
	}
}
