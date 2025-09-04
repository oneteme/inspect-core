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
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.createDatabaseRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.usf.inspect.core.DatabaseAction;
import org.usf.inspect.core.DatabaseCommand;
import org.usf.inspect.core.DatabaseRequest;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.StageArgsHolder;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class DatabaseRequestMonitor {

	private final ConnectionMetadataCache cache; //required
	private final DatabaseRequest req = createDatabaseRequest();
	
	private DatabaseCommand mainCommand;
	private boolean prepared;

	private StageArgsHolder stageHolder = new StageArgsHolder();
	private DatabaseRequestStage lastExec; // hold last exec stage
	
	public DatabaseRequest handleConnection(Instant start, Instant end, Connection cnx, Throwable thw) throws SQLException {
		req.createStage(CONNECTION, start, end, thw, null).emit();
		req.setThreadName(threadName());
		req.setStart(start);
		if(nonNull(thw)) { //if connection error
			req.setEnd(end);
		}
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
		return req;
	}
	
	public ExecutionHandler<Statement> statementStageHandler(String sql) {
		return (s,e,o,t)-> {
			mainCommand = null; //rest
			if(nonNull(sql)) {
				mainCommand = parseCommand(sql);
				prepared = true;
			}
			return req.createStage(STATEMENT, s, e, t, null); //sql.split.count ?
		};
	}

	public ExecutionHandler<Void> addBatchStageHandler(String sql) {
		return (s,e,o,t)-> {
			if(nonNull(sql)) { //statement
				mainCommand = mergeCommand(mainCommand, parseCommand(sql));
			}
			if(stageHolder.getAction() != BATCH) { //safe++
				stageHolder.set(BATCH, req.createStage(BATCH, s, e, t, null), new long[]{1});
			}
			else {
				var stg = stageHolder.getStage();
				stg.setEnd(e); //optim this
				if(nonNull(t)) {
					stg.setException(mainCauseException(t)); //may overwrite previous
				}
				stageHolder.getCount()[0]++;
			}
			return null; //do not emit trace here
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

	private <T> ExecutionHandler<T> executeStageHandler(String sql, Function<T, long[]> countFn) {
		if(stageHolder.getAction() == BATCH) { //batch & largeBatch
			var stg = stageHolder.getStage();
			stg.setCount(stageHolder.getCount());
			stg.emit(); //wait for last addBatch
			stageHolder.clear();
		}
		return (s,e,o,t)-> {
			if(nonNull(sql)) { //statement
				mainCommand = mergeCommand(mainCommand, parseCommand(sql));
			}
			lastExec = req.createStage(EXECUTE, s, e, t, mainCommand, nonNull(o) ? countFn.apply(o) : null); // o may be null, if execution failed
			if(!prepared) { //multiple batch execution
				mainCommand = null;
			}
			return lastExec;
		};
	}

	public <T> ExecutionHandler<T> fetch(Instant start, int n) {
		return (s,e,o,t)-> req.createStage(FETCH, start, e, t, null, new long[] {n}); //differed start 
	}

	public void updateStageRowsCount(long rows) {
//		if(rows > -1) {
//			try { //safe
//				if(nonNull(count)) {
//					var arr = lastExec.getCount();
//					lastExec.setCount(isNull(arr) ? new long[] {rows} : appendLong(arr, rows)); // getMoreResults
//				}
//			}
//			catch (Exception e) {
//				reportError("DatabaseRequestMonitor.updateStageRowsCount", req, e);
//			}
//		}
	}
	
	public DatabaseRequest handleDisconnection(Instant start, Instant end, Void v, Throwable t) { //sonar: used as lambda
		req.createStage(DISCONNECTION, start, end, t, null).emit();
		req.runSynchronized(()-> req.setEnd(end));
		return req;
	}

	public ExecutionHandler<Object> stageHandler(DatabaseAction action, String... args) {
		return stageHandler(action, null, args);
	}

	public ExecutionHandler<Object> stageHandler(DatabaseAction action, DatabaseCommand cmd, String... args) {
		return (s,e,o,t)-> req.createStage(action, s, e, t, cmd, args);
	}

	static DatabaseCommand mergeCommand(DatabaseCommand main, DatabaseCommand cmd) {
		if(main == cmd || isNull(cmd)) {
			return main;
		}
		return isNull(main) ? cmd : SQL;
	}
	
	static long[] appendLong(long[]arr, long v) {
		var a = copyOf(arr, arr.length+1);
		a[arr.length] = v;
		return a;
	}
}
