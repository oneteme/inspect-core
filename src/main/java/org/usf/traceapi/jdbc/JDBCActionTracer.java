package org.usf.traceapi.jdbc;

import static java.time.Instant.now;
import static java.util.Arrays.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.MetricsTracker.call;
import static org.usf.traceapi.core.MetricsTracker.supply;
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
import java.util.LinkedList;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.usf.traceapi.core.DatabaseRequestStage;
import org.usf.traceapi.core.SafeSupplier;
import org.usf.traceapi.core.SafeSupplier.MetricsConsumer;
import org.usf.traceapi.core.SafeSupplier.SafeRunnable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public class JDBCActionTracer {
	
	private final LinkedList<DatabaseRequestStage> actions = new LinkedList<>();
	private final LinkedList<SqlCommand> commands = new LinkedList<>();
	
	private DatabaseRequestStage exec;
	
	public ConnectionWrapper connection(SafeSupplier<Connection, SQLException> supplier) throws SQLException {
		return new ConnectionWrapper(supply(supplier, appendAction(CONNECTION)), this);
	}

	public void disconnection(SafeRunnable<SQLException> method) throws SQLException {
		call(method, appendAction(DISCONNECTION));
	}
	
	public StatementWrapper statement(SafeSupplier<Statement, SQLException> supplier) throws SQLException {
		return new StatementWrapper(supply(supplier, appendAction(STATEMENT)), this);
	}
	
	public PreparedStatementWrapper preparedStatement(String sql, SafeSupplier<PreparedStatement, SQLException> supplier) throws SQLException {
		return new PreparedStatementWrapper(supply(supplier, appendAction(STATEMENT)), this, sql); //parse command on exec
	}

	public DatabaseMetaData connectionMetadata(SafeSupplier<DatabaseMetaData, SQLException> supplier) throws SQLException {
		return supply(supplier, appendAction(METADATA));
	}

	public ResultSetMetaData resultSetMetadata(SafeSupplier<ResultSetMetaData, SQLException> supplier) throws SQLException {
		return supply(supplier, appendAction(METADATA));
	}

	public ResultSetWrapper resultSet(SafeSupplier<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(supplier.get(), this, now());  // no need to trace this
	}

	public ResultSetWrapper executeQuery(String sql, SafeSupplier<ResultSet, SQLException> supplier) throws SQLException {
		return new ResultSetWrapper(execute(sql, supplier, rs-> null), this, now()); // no count 
	}

	public boolean execute(String sql, SafeSupplier<Boolean, SQLException> supplier) throws SQLException {
		return execute(sql, supplier, b-> null);
	}
	
	public int executeUpdate(String sql, SafeSupplier<Integer, SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> new long[] {n});
	}
	
	public long executeLargeUpdate(String sql, SafeSupplier<Long, SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> new long[] {n});
	}

	public int[] executeBatch(String sql, SafeSupplier<int[], SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> IntStream.of(n).mapToLong(v-> v).toArray());
	}
	
	public long[] executeLargeBatch(String sql, SafeSupplier<long[], SQLException> supplier) throws SQLException {
		return execute(sql, supplier, n-> n);
	}

	private <T> T execute(String sql, SafeSupplier<T, SQLException> supplier, Function<T, long[]> countFn) throws SQLException {
		if(nonNull(sql)) {
			commands.add(mainCommand(sql));
		} //BATCH otherwise 
		return supply(supplier, appendAction(EXECUTE, countFn));
	}

	public <T> T savePoint(SafeSupplier<T, SQLException> supplier) throws SQLException {
		return supply(supplier, appendAction(SAVEPOINT));
	}

	public void addBatch(String sql, SafeRunnable<SQLException> method) throws SQLException {
		if(nonNull(sql)) {
			commands.add(mainCommand(sql));
		} // PreparedStatement otherwise 
		call(method, nonNull(sql) || actions.isEmpty() || !BATCH.name().equals(actions.getLast().getName())
				? appendAction(BATCH, v-> new long[] {1})  //statement | first batch
				: this::updateLast);
	}
	
	public void commit(SafeRunnable<SQLException> method) throws SQLException {
		call(method, appendAction(COMMIT));
	}
	
	public void rollback(SafeRunnable<SQLException> method) throws SQLException {
		call(method, appendAction(ROLLBACK));
	}
	
	public void fetch(Instant start, SafeRunnable<SQLException> method, int n) throws SQLException {
		call(()-> start, method, appendAction(FETCH, v-> new long[] {n}));
	}

	public boolean moreResults(Statement st, SafeSupplier<Boolean, SQLException> supplier) throws SQLException {
		if(supplier.get()) { // no need to trace this
			if(nonNull(exec)) {
				try {
					var rows = st.getUpdateCount();
					if(rows > -1) {
						var arr = exec.getCount();
						exec.setCount(isNull(arr) ? new long[] {rows} : appendLong(arr, rows));
					}
				}
				catch (Exception e) {log.warn("getUpdateCount => {}", e.getMessage());}
			}
			return true;
		}
		return false;
	}

	void updateLast(Instant start, Instant end, Void v, Throwable t) {
		var action = actions.getLast();
		action.setEnd(end); // shift end
		if(nonNull(t) && isNull(action.getException())) {
			action.setException(mainCauseException(t));
		} //else illegal state
		action.getCount()[0]++;
	}

	<T> MetricsConsumer<T> appendAction(JDBCAction action) {
		return appendAction(action, null);
	}
	
	<T> MetricsConsumer<T> appendAction(JDBCAction action, Function<T, long[]> countFn) {
		return (s,e,o,t)->{
			var rs = new DatabaseRequestStage();
			rs.setName(action.name());
			rs.setStart(s);
			rs.setEnd(e);
			rs.setException(mainCauseException(t));
			if(nonNull(countFn)) {
				rs.setCount(countFn.apply(o));
			}
			actions.add(rs);
		};
	}
	
	static long[] appendLong(long[]arr, long v) {
		var a = copyOf(arr, arr.length+1);
		a[arr.length] = v;
		return a;
	}
}
