package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static org.usf.traceapi.core.Action.BATCH;
import static org.usf.traceapi.core.Action.COMMIT;
import static org.usf.traceapi.core.Action.CONNECTION;
import static org.usf.traceapi.core.Action.EXECUTE;
import static org.usf.traceapi.core.Action.FETCH;
import static org.usf.traceapi.core.Action.METADATA;
import static org.usf.traceapi.core.Action.RESULTSET;
import static org.usf.traceapi.core.Action.ROLLBACK;
import static org.usf.traceapi.core.Action.SAVEPOINT;
import static org.usf.traceapi.core.Action.SELECT;
import static org.usf.traceapi.core.Action.STATEMENT;
import static org.usf.traceapi.core.Action.UPDATE;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.log;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface DatabaseActionTracer extends Consumer<DatabaseAction> {
	
	default ConnectionWrapper connection(SQLSupplier<Connection> supplier) throws SQLException {
		return new ConnectionWrapper(trace(CONNECTION, supplier), this);
	}
	
	default DatabaseMetaData connectionMetadata(SQLSupplier<DatabaseMetaData> supplier) throws SQLException {
		return trace(METADATA, supplier);
	}

	default StatementWrapper statement(SQLSupplier<Statement> supplier) throws SQLException {
		return new StatementWrapper(trace(STATEMENT, supplier), this);
	}
	
	default PreparedStatementWrapper preparedStatement(SQLSupplier<PreparedStatement> supplier) throws SQLException {
		return new PreparedStatementWrapper(trace(STATEMENT, supplier), this);
	}
	
	default ResultSetWrapper select(SQLSupplier<ResultSet> supplier) throws SQLException {
		return resultset(supplier, SELECT);
	}
	
	default ResultSetWrapper resultset(SQLSupplier<ResultSet> supplier) throws SQLException {
		return resultset(supplier, RESULTSET);
	}
	
	private ResultSetWrapper resultset(SQLSupplier<ResultSet> supplier, Action action) throws SQLException {
		return new ResultSetWrapper(trace(action, supplier), this, currentTimeMillis());
	}
	
	default ResultSetMetaData resultSetMetadata(SQLSupplier<ResultSetMetaData> supplier) throws SQLException {
		return trace(METADATA, supplier);
	}
	
	default <T> T execute(SQLSupplier<T> supplier) throws SQLException {
		return trace(EXECUTE, supplier);
	}

	default <T> T update(SQLSupplier<T> supplier) throws SQLException {
		return trace(UPDATE, supplier);
	}

	default <T> T batch(SQLSupplier<T> supplier) throws SQLException {
		return trace(BATCH, supplier);
	}
	
	default <T> T savePoint(SQLSupplier<T> supplier) throws SQLException {
		return trace(SAVEPOINT, supplier);
	}
	
	default void commit(SQLMethod method) throws SQLException {
		trace(COMMIT, method::callAsSupplier);
	}
	
	default void rollback(SQLMethod method) throws SQLException {
		trace(ROLLBACK, method::callAsSupplier);
	}
	
	default void fetch(long start, SQLMethod method) throws SQLException {
		trace(FETCH, ()-> start, method::callAsSupplier); // differed start
	}

	default <T> T trace(Action action, SQLSupplier<T> sqlSupp) throws SQLException {
		return trace(action, System::currentTimeMillis, sqlSupp);
	}

	private <T> T trace(Action action, LongSupplier startSupp, SQLSupplier<T> sqlSupp) throws SQLException {
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
			accept(new DatabaseAction(action, ofEpochMilli(beg), ofEpochMilli(fin), mainCauseException(ex)));
		}
	}

	@FunctionalInterface
	public interface SQLSupplier<T> {
		
		T get() throws SQLException;
	}
	
	@FunctionalInterface
	public interface SQLMethod {
		
		void call() throws SQLException;
		
		default Void callAsSupplier() throws SQLException {
			this.call();
			return null;
		}
	}	
}
