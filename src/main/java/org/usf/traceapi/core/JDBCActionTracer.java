package org.usf.traceapi.core;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Arrays.copyOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.JDBCAction.BATCH;
import static org.usf.traceapi.core.JDBCAction.COMMIT;
import static org.usf.traceapi.core.JDBCAction.CONNECTION;
import static org.usf.traceapi.core.JDBCAction.EXECUTE;
import static org.usf.traceapi.core.JDBCAction.FETCH;
import static org.usf.traceapi.core.JDBCAction.METADATA;
import static org.usf.traceapi.core.JDBCAction.ROLLBACK;
import static org.usf.traceapi.core.JDBCAction.SAVEPOINT;
import static org.usf.traceapi.core.JDBCAction.STATEMENT;
import static org.usf.traceapi.core.SqlCommand.mainCommand;

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
import java.util.function.Supplier;
import java.util.stream.IntStream;

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
	
	private final LinkedList<DatabaseAction> actions = new LinkedList<>();
	private final LinkedList<SqlCommand> commands = new LinkedList<>();
	
	private DatabaseAction exec;
	
	public ConnectionWrapper connection(SQLSupplier<Connection> supplier) throws SQLException {
		return new ConnectionWrapper(trace(CONNECTION, supplier), this);
	}
	
	public StatementWrapper statement(SQLSupplier<Statement> supplier) throws SQLException {
		return new StatementWrapper(trace(STATEMENT, supplier), this);
	}
	
	public PreparedStatementWrapper preparedStatement(String sql, SQLSupplier<PreparedStatement> supplier) throws SQLException {
		return new PreparedStatementWrapper(trace(STATEMENT, supplier), this, sql); //parse command on exec
	}

	public DatabaseMetaData connectionMetadata(SQLSupplier<DatabaseMetaData> supplier) throws SQLException {
		return trace(METADATA, supplier);
	}

	public ResultSetMetaData resultSetMetadata(SQLSupplier<ResultSetMetaData> supplier) throws SQLException {
		return trace(METADATA, supplier);
	}

	public boolean execute(String sql, SQLSupplier<Boolean> supplier) throws SQLException {
		var b = execute(EXECUTE, sql, supplier);
		this.exec = actions.getLast(); 
		return b;
	}

	public ResultSetWrapper executeQuery(String sql, SQLSupplier<ResultSet> supplier) throws SQLException {
		return new ResultSetWrapper(execute(EXECUTE, sql, supplier), this, now()); // no count 
	}
	
	public int executeUpdate(String sql, SQLSupplier<Integer> supplier) throws SQLException {
		return execute(sql, supplier, n-> new long[] {n});
	}
	
	public long executeLargeUpdate(String sql, SQLSupplier<Long> supplier) throws SQLException {
		return execute(sql, supplier, n-> new long[] {n});
	}

	public int[] executeBatch(String sql, SQLSupplier<int[]> supplier) throws SQLException {
		return execute(sql, supplier, n-> IntStream.of(n).mapToLong(v-> v).toArray());
	}
	
	public long[] executeLargeBatch(String sql, SQLSupplier<long[]> supplier) throws SQLException {
		return execute(sql, supplier, n-> n);
	}

	private <T> T execute(String sql, SQLSupplier<T> supplier, Function<T, long[]> countFn) throws SQLException  {
		var rows = execute(EXECUTE, sql, supplier);
		actions.getLast().setCount(countFn.apply(rows));
		return rows;
	}

	private <T> T execute(JDBCAction action, String sql, SQLSupplier<T> supplier) throws SQLException {
		if(nonNull(sql)) {
			commands.add(mainCommand(sql));
		} //BATCH otherwise 
		return trace(action, supplier);
	}

	public <T> T savePoint(SQLSupplier<T> supplier) throws SQLException {
		return trace(SAVEPOINT, supplier);
	}

	public void addBatch(String sql, SQLMethod method) throws SQLException {
		if(nonNull(sql)) {
			commands.add(mainCommand(sql));
		} // PreparedStatement otherwise 
		trace(BATCH, Instant::now, method, this::tryUpdatePrevious);
	}
	
	public void commit(SQLMethod method) throws SQLException {
		trace(COMMIT, method);
	}
	
	public void rollback(SQLMethod method) throws SQLException {
		trace(ROLLBACK, method);
	}
	
	public void fetch(Instant start, SQLMethod method, int n) throws SQLException {
		trace(FETCH, ()-> start, method, this::append); // differed start
		actions.getLast().setCount(new long[] {n});
	}
	
	<T> T trace(JDBCAction action, SQLSupplier<T> sqlSupp) throws SQLException {
		return trace(action, Instant::now, sqlSupp, this::append);
	}

	private <T> T trace(JDBCAction action, Supplier<Instant> startSupp, SQLSupplier<T> sqlSupp, DatabaseActionConsumer cons) throws SQLException {
		log.trace("executing {} action..", action);
		SQLException ex = null;
		var beg = startSupp.get();
		try {
			return sqlSupp.get();
		}
		catch(SQLException e) {
			ex  = e;
			throw e;
		}
		finally {
			var fin = now();
			cons.accept(action, beg, fin, mainCauseException(ex));
		}
	}

	public boolean moreResults(Statement st, SQLSupplier<Boolean> supplier) throws SQLException {
		try {
			return supplier.get(); // no need to trace this
		}
		finally {
			if(nonNull(exec)) {
				try {
					var rows = st.getUpdateCount();
					if(rows > -1) {
						var arr = exec.getCount();
						exec.setCount(isNull(arr) ? new long[] {rows} : appendLong(arr, rows)); //differed call
					}
				}
				catch (Exception e) {
					//do not throw exception
				}
			}
		}
	}
	
	@FunctionalInterface
	public interface SQLSupplier<T> {
		
		T get() throws SQLException;
	}
	
	@FunctionalInterface
	public interface SQLMethod extends SQLSupplier<Void> {
		
		void call() throws SQLException;
		
		default Void get() throws SQLException {
			this.call();
			return null;
		}
	}
	
	@FunctionalInterface
	public interface DatabaseActionConsumer {
		
		void accept(JDBCAction action, Instant start, Instant end, ExceptionInfo ex);
	}

	void tryUpdatePrevious(JDBCAction type, Instant start, Instant end, ExceptionInfo ex) {
		if(!actions.isEmpty() && actions.getLast().getType() == type && MILLIS.between(actions.getLast().getEnd(), start) < 2) { //config!?
			var action = actions.getLast();
			if(nonNull(ex) && isNull(action.getException())) {
				action.setException(ex);
			}
			action.setEnd(end);
			action.getCount()[0]++;
		}
		else {
			append(type, start, end, ex);
		}
	}
	
	void append(JDBCAction type, Instant start, Instant end, ExceptionInfo ex) {
		actions.add(new DatabaseAction(type, start, end, ex));
	}
	
	static long[] appendLong(long[]arr, long v) {
		var a = copyOf(arr, arr.length);
		a[arr.length] = v;
		return a;
	}
}
