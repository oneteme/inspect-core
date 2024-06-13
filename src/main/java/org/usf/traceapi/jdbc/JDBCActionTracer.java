package org.usf.traceapi.jdbc;

import static java.time.Instant.now;
import static java.util.Arrays.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.usf.traceapi.core.DatabaseRequest.newDatabaseRequest;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.stackTraceElement;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.Session.appendSessionStage;
import static org.usf.traceapi.core.StageTracker.call;
import static org.usf.traceapi.core.StageTracker.exec;
import static org.usf.traceapi.jdbc.JDBCAction.BATCH;
import static org.usf.traceapi.jdbc.JDBCAction.COMMIT;
import static org.usf.traceapi.jdbc.JDBCAction.CONNECTION;
import static org.usf.traceapi.jdbc.JDBCAction.DISCONNECTION;
import static org.usf.traceapi.jdbc.JDBCAction.EXECUTE;
import static org.usf.traceapi.jdbc.JDBCAction.FETCH;
import static org.usf.traceapi.jdbc.JDBCAction.METADATA;
import static org.usf.traceapi.jdbc.JDBCAction.ROLLBACK;
import static org.usf.traceapi.jdbc.JDBCAction.SAVEPOINT;
import static org.usf.traceapi.jdbc.JDBCAction.STATEMENT;
import static org.usf.traceapi.jdbc.SqlCommand.mainCommand;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.usf.traceapi.core.DatabaseRequest;
import org.usf.traceapi.core.DatabaseRequestStage;
import org.usf.traceapi.core.SafeCallable;
import org.usf.traceapi.core.SafeCallable.SafeRunnable;
import org.usf.traceapi.core.StageTracker.StageConsumer;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public class JDBCActionTracer {
	
	private static final Pattern hostPattern = compile("^jdbc:[\\w:]+@?//([-\\w\\.]+)(:(\\d+))?(/(\\w+)|/(\\w+)[\\?,;].*|.*)$", CASE_INSENSITIVE);
	private static final Pattern dbPattern = compile("database=(\\w+)", CASE_INSENSITIVE);
	
	private DatabaseRequest req = newDatabaseRequest(); //avoid nullPointer
	private DatabaseRequestStage exec; //last execute
	
	public ConnectionWrapper connection(SafeCallable<Connection, SQLException> supplier) throws SQLException {
		return new ConnectionWrapper(call(supplier, (s,e,cn,t)->{
			req = newDatabaseRequest();
			req.setStart(s);
			if(nonNull(t)) { // fail
				req.setEnd(e);
				//do not setException, already set in action
			}
			req.setThreadName(threadName());
			stackTraceElement().ifPresent(st->{
				req.setName(st.getMethodName());
				req.setLocation(st.getClassName());
			});
			if(nonNull(cn)) {
				var meta = cn.getMetaData();
				var args = decodeURL(meta.getURL());
				req.setHost(args[0]);
				req.setPort(ofNullable(args[1]).map(Integer::parseInt).orElse(-1));
				req.setDatabase(args[2]);
				req.setUser(meta.getUserName());
				req.setDatabaseName(meta.getDatabaseProductName());
				req.setDatabaseVersion(meta.getDatabaseProductVersion());
				req.setDriverVersion(meta.getDriverVersion());
			}
			appendAction(CONNECTION).accept(s, e, cn, t);
			appendSessionStage(req);
		}), this);
	}

	public void disconnection(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, (s,e,v,t)->{
			appendAction(DISCONNECTION).accept(s, e, v, t);
			req.setEnd(e);
		});
	}
	
	public StatementWrapper statement(SafeCallable<Statement, SQLException> supplier) throws SQLException {
		return new StatementWrapper(call(supplier, appendAction(STATEMENT)), this);
	}
	
	public PreparedStatementWrapper preparedStatement(String sql, SafeCallable<PreparedStatement, SQLException> supplier) throws SQLException {
		return new PreparedStatementWrapper(call(supplier, appendAction(STATEMENT)), this, sql); //parse command on exec
	}

	public DatabaseMetaData connectionMetadata(SafeCallable<DatabaseMetaData, SQLException> supplier) throws SQLException {
		return call(supplier, appendAction(METADATA));
	}

	public ResultSetMetaData resultSetMetadata(SafeCallable<ResultSetMetaData, SQLException> supplier) throws SQLException {
		return call(supplier, appendAction(METADATA));
	}

	public ResultSetWrapper resultSet(SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(supplier.call(), this, now());  // no need to trace this
	}

	public ResultSetWrapper executeQuery(String sql, SafeCallable<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(execute(sql, supplier, rs-> null), this, now()); // no count 
	}

	public boolean execute(String sql, SafeCallable<Boolean, SQLException> supplier) throws SQLException {
		return execute(sql, supplier, b-> null);
	}
	
	public int executeUpdate(String sql, SafeCallable<Integer, SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> new long[] {n});
	}
	
	public long executeLargeUpdate(String sql, SafeCallable<Long, SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> new long[] {n});
	}

	public int[] executeBatch(String sql, SafeCallable<int[], SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> IntStream.of(n).mapToLong(v-> v).toArray());
	}
	
	public long[] executeLargeBatch(String sql, SafeCallable<long[], SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> n);
	}

	private <T> T execute(String sql, SafeCallable<T, SQLException> supplier, Function<T, long[]> countFn) throws SQLException {
		if(nonNull(sql)) {
			req.getCommands().add(mainCommand(sql));
		} //BATCH otherwise 
		return call(supplier, appendAction(EXECUTE, (a,r)-> a.setCount(countFn.apply(r))));
	}

	public <T> T savePoint(SafeCallable<T, SQLException> supplier) throws SQLException {
		return call(supplier, appendAction(SAVEPOINT));
	}

	public void addBatch(String sql, SafeRunnable<SQLException> method) throws SQLException {
		if(nonNull(sql)) {
			req.getCommands().add(mainCommand(sql));
		} // PreparedStatement otherwise 
		exec(method, nonNull(sql) || req.getActions().isEmpty() || !BATCH.name().equals(last(req.getActions()).getName())
				? appendAction(BATCH, (a,v)-> a.setCount(new long[] {1})) //statement | first batch
				: this::updateLast);
	}
	
	public void commit(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, appendAction(COMMIT));
	}
	
	public void rollback(SafeRunnable<SQLException> method) throws SQLException {
		exec(method, appendAction(ROLLBACK));
	}
	
	public void fetch(Instant start, SafeRunnable<SQLException> method, int n) throws SQLException {
		exec(method, appendAction(FETCH, (a, v)-> {
			a.setStart(start); // differed start
			a.setCount(new long[] {n});
		}));
	}

	public boolean moreResults(Statement st, SafeCallable<Boolean, SQLException> supplier) throws SQLException {
		if(supplier.call()) { // no need to trace this
			if(nonNull(exec)) {
				try {
					var rows = st.getUpdateCount();
					if(rows > -1) {
						var arr = exec.getCount();
						exec.setCount(isNull(arr) ? new long[] {rows} : appendLong(arr, rows));
					}
				}
				catch (Exception e) {
					log.warn("getUpdateCount => {}", e.getMessage());
				}
			}
			return true;
		}
		return false;
	}

	void updateLast(Instant start, Instant end, Void v, Throwable t) {
		var action = last(req.getActions());
		action.setEnd(end); // shift end
		if(nonNull(t) && isNull(action.getException())) {
			action.setException(mainCauseException(t));
		} //else illegal state
		action.getCount()[0]++;
	}

	<T> StageConsumer<T> appendAction(JDBCAction action) {
		return appendAction(action, null);
	}
	
	<T> StageConsumer<T> appendAction(JDBCAction action, BiConsumer<DatabaseRequestStage, T> cons) {
		return (s,e,o,t)->{
			var stg = new DatabaseRequestStage();
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			stg.setException(mainCauseException(t));
			if(nonNull(cons)) {
				cons.accept(stg,o);
			}
			req.getActions().add(stg);
		};
	}
	
	static long[] appendLong(long[]arr, long v) {
		var a = copyOf(arr, arr.length+1);
		a[arr.length] = v;
		return a;
	}
	
	static String[] decodeURL(String url) {
		var m = hostPattern.matcher(url);
		String[] arr = new String[3];
		if(m.find()) {
			arr[0] = m.group(1);
			arr[1] = m.group(3);
			int i = 5;
			while(i<=m.groupCount() && isNull(arr[2] = m.group(i++)));
		}
		if(isNull(arr[2])) {
			m = dbPattern.matcher(url);
			if(m.find()) {
				arr[2] = m.group(1);
			}
		}
		return arr;
	}
	
	private static <T> T last(List<T> list) { //safe
		return list.get(list.size()-1);
	}
}
