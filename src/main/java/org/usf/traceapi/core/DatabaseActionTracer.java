package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.SqlAction.BATCH;
import static org.usf.traceapi.core.SqlAction.COMMIT;
import static org.usf.traceapi.core.SqlAction.CONNECTION;
import static org.usf.traceapi.core.SqlAction.EXECUTE;
import static org.usf.traceapi.core.SqlAction.FETCH;
import static org.usf.traceapi.core.SqlAction.METADATA;
import static org.usf.traceapi.core.SqlAction.RESULTSET;
import static org.usf.traceapi.core.SqlAction.ROLLBACK;
import static org.usf.traceapi.core.SqlAction.SAVEPOINT;
import static org.usf.traceapi.core.SqlAction.STATEMENT;
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
import java.util.function.LongSupplier;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public class DatabaseActionTracer {
	
	private final LinkedList<DatabaseAction> actions = new LinkedList<>();
	private final LinkedList<SqlCommand> commands = new LinkedList<>();
	
	public ConnectionWrapper connection(SQLSupplier<Connection> supplier) throws SQLException {
		return new ConnectionWrapper(trace(CONNECTION, supplier), this);
	}
	
	public DatabaseMetaData connectionMetadata(SQLSupplier<DatabaseMetaData> supplier) throws SQLException {
		return trace(METADATA, supplier);
	}

	public StatementWrapper statement(SQLSupplier<Statement> supplier) throws SQLException {
		return new StatementWrapper(trace(STATEMENT, supplier), this);
	}
	
	public PreparedStatementWrapper preparedStatement(String sql, SQLSupplier<PreparedStatement> supplier) throws SQLException {
		return new PreparedStatementWrapper(trace(STATEMENT, supplier), this, sql); //do not parse command here
	}
	
	public ResultSetWrapper resultSet(SQLSupplier<ResultSet> supplier) throws SQLException {
		return resultSet(RESULTSET, supplier);
	}
	
	public ResultSetWrapper executeQuery(String sql, SQLSupplier<ResultSet> supplier) throws SQLException {
		commands.add(mainCommand(sql)); //must be SELECT
		return resultSet(EXECUTE, supplier);
	}
	
	private ResultSetWrapper resultSet(SqlAction action, SQLSupplier<ResultSet> supplier) throws SQLException {
		return new ResultSetWrapper(trace(action, supplier), this, currentTimeMillis());
	}
	
	public ResultSetMetaData resultSetMetadata(SQLSupplier<ResultSetMetaData> supplier) throws SQLException {
		return trace(METADATA, supplier);
	}

	public <T> T execute(String sql, SQLSupplier<T> supplier) throws SQLException {
		if(nonNull(sql)) {
			commands.add(mainCommand(sql));
		} // PreparedStatement | BATCH otherwise 
		return trace(EXECUTE, supplier);
	}

	public <T> T savePoint(SQLSupplier<T> supplier) throws SQLException {
		return trace(SAVEPOINT, supplier);
	}

	public void batch(String sql, SQLMethod method) throws SQLException {
		if(nonNull(sql)) {
			commands.add(mainCommand(sql));
		} // PreparedStatement otherwise 
		trace(BATCH, method, this::tryUpdatePrevious);
	}
	
	public void commit(SQLMethod method) throws SQLException {
		trace(COMMIT, method);
	}
	
	public void rollback(SQLMethod method) throws SQLException {
		trace(ROLLBACK, method);
	}
	
	public void fetch(long start, SQLMethod method) throws SQLException {
		trace(FETCH, ()-> start, method, this::append); // differed start
	}
	
	private <T> T trace(SqlAction action, SQLSupplier<T> sqlSupp) throws SQLException {
		return trace(action, System::currentTimeMillis, sqlSupp, this::append);
	}

	private <T> T trace(SqlAction action, SQLSupplier<T> sqlSupp, DatabaseActionConsumer cons) throws SQLException {
		return trace(action, System::currentTimeMillis, sqlSupp, cons);
	}

	private <T> T trace(SqlAction action, LongSupplier startSupp, SQLSupplier<T> sqlSupp, DatabaseActionConsumer cons) throws SQLException {
		log.trace("executing {} action..", action);
		SQLException ex = null;
		var beg = startSupp.getAsLong();
		try {
			return sqlSupp.get();
		}
		catch(SQLException e) {
			ex  = e;
			throw e;
		}
		finally {
			var fin = currentTimeMillis();
			cons.accept(action, ofEpochMilli(beg), ofEpochMilli(fin), mainCauseException(ex));
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
		
		void accept(SqlAction action, Instant start, Instant end, ExceptionInfo ex);
	}

	void tryUpdatePrevious(SqlAction type, Instant start, Instant end, ExceptionInfo ex) {
		if(!actions.isEmpty()) {
			var action = actions.getLast();
			if(action.getType() == type && MILLIS.between(action.getEnd(), start) < 2) { //config
				action.setEnd(end);
				action.setCount(action.getCount()+1);
				if(nonNull(ex) && isNull(action.getException())) {
					action.setException(ex);
				}
			}
			else {
				append(type, start, end, ex);
			}
		}
		else {
			append(type, start, end, ex);
		}
	}
	
	void append(SqlAction type, Instant start, Instant end, ExceptionInfo ex) {
		actions.add(new DatabaseAction(type, start, end, ex, 1));
	}
}
