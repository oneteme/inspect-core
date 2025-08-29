package org.usf.inspect.jdbc;

import static java.util.Arrays.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorReporter.reportError;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.createDatabaseRequest;
import static org.usf.inspect.jdbc.JDBCAction.BATCH;
import static org.usf.inspect.jdbc.JDBCAction.CONNECTION;
import static org.usf.inspect.jdbc.JDBCAction.DISCONNECTION;
import static org.usf.inspect.jdbc.JDBCAction.EXECUTE;
import static org.usf.inspect.jdbc.JDBCAction.FETCH;
import static org.usf.inspect.jdbc.JDBCAction.STATEMENT;
import static org.usf.inspect.jdbc.SqlCommand.mainCommand;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.usf.inspect.core.DatabaseRequest;
import org.usf.inspect.core.DatabaseRequestStage;
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
	
	private List<SqlCommand> commands;
	private boolean prepared;

	private DatabaseRequestStage lastExec; // hold last exec stage
	private DatabaseRequestStage lastBatch; // hold last batch stage
	
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
			commands = new ArrayList<>(1);
			if(nonNull(sql)) {
				appendCommand(sql);
				prepared = true;
			}
			return req.createStage(STATEMENT, s, e, t, null); //sql.split.count ?
		};
	}

	public ExecutionHandler<Void> addBatchStageHandler(String sql) {
		return (s,e,o,t)-> {
			if(nonNull(sql)) { //statement
				appendCommand(sql);
			}
			if(isNull(lastBatch)) { //safe++
				lastBatch = req.createStage(BATCH, s, e, t, new long[]{1}); //submit on execute
			}
			else {
				lastBatch.setEnd(e); //optim this
				lastBatch.getCount()[0]++;
				if(nonNull(t)) {
					lastBatch.setException(mainCauseException(t)); //may overwrite previous
				}
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
		if(nonNull(lastBatch)) { //batch & largeBatch
			lastBatch.emit(); //wait for last addBatch
			lastBatch = null;
		}
		return (s,e,o,t)-> {
			if(nonNull(sql)) { //statement
				appendCommand(sql);
			}
			lastExec = req.createStage(EXECUTE, s, e, t, nonNull(o) ? countFn.apply(o) : null); // o may be null, if execution failed
			lastExec.setCommands(commands.toArray(SqlCommand[]::new));
			if(!prepared) { //multiple batch execution
				commands = new ArrayList<>();
			}
			return lastExec;
		};
	}

	public <T> ExecutionHandler<T> fetch(Instant start, int n) {
		return (s,e,o,t)-> req.createStage(FETCH, start, e, t, new long[] {n}); //differed start 
	}

	public void updateStageRowsCount(long rows) {
		if(rows > -1) {
			try { //safe
				if(nonNull(lastExec)) {
					var arr = lastExec.getCount();
					lastExec.setCount(isNull(arr) ? new long[] {rows} : appendLong(arr, rows)); // getMoreResults
				}
			}
			catch (Exception e) {
				reportError("DatabaseRequestMonitor.updateStageRowsCount", req, e);
			}
		}
	}
	
	public DatabaseRequest handleDisconnection(Instant start, Instant end, Void v, Throwable t) { //sonar: used as lambda
		req.createStage(DISCONNECTION, start, end, t, null).emit();
		req.runSynchronized(()-> req.setEnd(end));
		return req;
	}

	public ExecutionHandler<Object> stageHandler(JDBCAction action) {
		return (s,e,o,t)-> req.createStage(action, s, e, t, null);
	}

	public ExecutionHandler<Object> stageHandler(JDBCAction action, SqlCommand cmd) {
		return (s,e,o,t)-> {
			req.updateCommand(cmd);
			var stg = req.createStage(action, s, e, t, null);
			stg.setCommands(new SqlCommand[] {cmd});
			return stg;
		};
	}

	void appendCommand(String sql) {
		var cmd = mainCommand(sql);
		commands.add(cmd);
		req.updateCommand(cmd);
	}
	
	static long[] appendLong(long[]arr, long v) {
		var a = copyOf(arr, arr.length+1);
		a[arr.length] = v;
		return a;
	}
}
