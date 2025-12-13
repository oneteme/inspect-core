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
import static org.usf.inspect.core.DatabaseCommand.mergeCommand;
import static org.usf.inspect.core.DatabaseCommand.parseCommand;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.SessionContextManager.createDatabaseRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.usf.inspect.core.DatabaseAction;
import org.usf.inspect.core.DatabaseCommand;
import org.usf.inspect.core.DatabaseRequestCallback;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.Monitor;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class DatabaseRequestMonitor implements Monitor {

	private final ConnectionMetadataCache cache; //required

	private DatabaseRequestCallback callback;
	private boolean prepared;
	private DatabaseCommand mainCommand;
	private DatabaseRequestStage lastStg; // hold last stage
	
	public void handleConnection(Instant start, Instant end, Connection cnx, Throwable thrw) throws SQLException {
		callback = createDatabaseRequest(start, req->{
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
		});
		emit(callback.createStage(CONNECTION, start, end, thrw, null)); //before end if thrw
		if(nonNull(thrw)) { //if connection error
			callback.setEnd(end);
			emit(callback);
			callback = null;
		}
	}
	
	public ExecutionHandler<Statement> statementStageHandler(String sql) {
		return (s,e,o,t)-> {
			mainCommand = null; //rest
			if(nonNull(sql)) {
				mainCommand = parseCommand(sql);
				prepared = true;
			}
			if(assertStillOpened(callback)) { //report if request was closed
				emit(callback.createStage(STATEMENT, s, e, t, null)); //sql.split.count ?
			}
		};
	}

	public ExecutionHandler<Void> addBatchStageHandler(String sql) {
		return (s,e,o,t)-> {
			if(nonNull(sql)) { //statement
				mainCommand = mergeCommand(mainCommand, parseCommand(sql)); //command set on exec stg
			}
			if(nonNull(lastStg) && BATCH.name().equals(lastStg.getName())) { //safe++
				if(nonNull(t)) {
					lastStg.setException(mainCauseException(t)); //may overwrite previous
					emit(lastStg);
					lastStg = null;
				}
				else {
					lastStg.getCount()[0]++;
				}
				lastStg.setEnd(e); //optim this
			}
			else if(assertStillOpened(callback)) {//report if request was closed
				lastStg = callback.createStage(BATCH, s, e, t, null, new long[]{1});
			}
		};
	}
	
	public ExecutionHandler<ResultSet> executeQueryStageHandler(String sql) {
		return executeStageHandler(sql, rs-> null); // no count 
	}

	public ExecutionHandler<Boolean> executeStageHandler(String sql) {
		return executeStageHandler(sql, b-> null); //-1 if select, cannot call  getUpdateCount
	}

	public ExecutionHandler<Integer> executeUpdateStageHandler(String sql) {
		return executeStageHandler(sql, n-> new long[] {n});
	}

	public ExecutionHandler<Long> executeLargeUpdateStageHandler(String sql) {
		return executeStageHandler(sql, n-> new long[] {n});
	}

	public ExecutionHandler<int[]> executeBatchStageHandler(){
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

	public ExecutionHandler<long[]> executeLargeBatchStageHandler() {
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
		if(nonNull(lastStg) && BATCH.name().equals(lastStg.getName())) { //batch & largeBatch
			emit(lastStg);
			lastStg = null;
		}
		else {
			reportMessage(false, "emitBatchStage", "empty batch or already traced");
		}
	}

	private <T> ExecutionHandler<T> executeStageHandler(String sql, Function<T, long[]> countFn) {
		return (s,e,o,t)-> {
			if(nonNull(sql)) { //statement
				mainCommand = mergeCommand(mainCommand, parseCommand(sql)); //command set on exec stg
			}
			if(assertStillOpened(callback)) { //report if request was closed
				lastStg = callback.createStage(EXECUTE, s, e, t, mainCommand, nonNull(o) ? countFn.apply(o) : null); // o may be null, if execution failed
				emit(lastStg);
			}
			if(!prepared) { //else multiple preparedStmt execution
				mainCommand = null;
			}
		};
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
				reportError(false, "DatabaseRequestMonitor.updateStageRowsCount", e);
			}
		}
	}

	public <T> ExecutionHandler<T> fetch(Instant start, int n) {
		return (s,e,o,t)-> {
			if(assertStillOpened(callback)) { //report if request was closed
				emit(callback.createStage(FETCH, start, e, t, null, new long[] {n})); //differed start 
			}
		};
	}
	
	public void handleDisconnection(Instant start, Instant end, Void v, Throwable t) { //sonar: used as lambda
		if(assertStillOpened(callback)) {  //report if request was closed, avoid emit trace twice
			emit(callback.createStage(DISCONNECTION, start, end, t, null));
			callback.setEnd(end);
			emit(callback);
			callback = null;
		}
	}

	public ExecutionHandler<Object> stageHandler(DatabaseAction action, String... args) {
		return stageHandler(action, null, args);
	}

	public ExecutionHandler<Object> stageHandler(DatabaseAction action, DatabaseCommand cmd, String... args) {
		return (s,e,o,t)-> {
			if(assertStillOpened(callback)) { //report if request was closed
				emit(callback.createStage(action, s, e, t, cmd, args));
			}
		};
	}
	
	static long[] appendLong(long[]arr, long v) {
		var a = copyOf(arr, arr.length+1);
		a[arr.length] = v;
		return a;
	}
}
