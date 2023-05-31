package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static org.usf.traceapi.core.Action.BATCH;
import static org.usf.traceapi.core.Action.CONNECTION;
import static org.usf.traceapi.core.Action.FETCH;
import static org.usf.traceapi.core.Action.SELECT;
import static org.usf.traceapi.core.Action.SQL;
import static org.usf.traceapi.core.Action.STATEMENT;
import static org.usf.traceapi.core.Action.UPDATE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

	default StatementWrapper statement(SQLSupplier<Statement> supplier) throws SQLException {
		return new StatementWrapper(trace(STATEMENT, supplier), this);
	}
	
	default PreparedStatementWrapper preparedStatement(SQLSupplier<PreparedStatement> supplier) throws SQLException {
		return new PreparedStatementWrapper(trace(STATEMENT, supplier), this);
	}
	
	default ResultSetWrapper select(SQLSupplier<ResultSet> supplier) throws SQLException {
		var rs = trace(SELECT, supplier);
		return new ResultSetWrapper(rs, this, currentTimeMillis());
	}
	
	default <T> T sql(SQLSupplier<T> supplier) throws SQLException {
		return trace(SQL, supplier);
	}

	default <T> T update(SQLSupplier<T> supplier) throws SQLException {
		return trace(UPDATE, supplier);
	}

	default <T> T batch(SQLSupplier<T> supplier) throws SQLException {
		return trace(BATCH, supplier);
	}
	
	default void fetch(long start, SQLMethod method) throws SQLException {
		trace(FETCH, ()-> start, method::callAsSupplier); // differed start
	}

	default <T> T trace(Action action, SQLSupplier<T> sqlSupp) throws SQLException {
		return trace(action, System::currentTimeMillis, sqlSupp);
	}

	private <T> T trace(Action action, LongSupplier startSupp, SQLSupplier<T> sqlSupp) throws SQLException {
		var err = true;
		var beg = startSupp.getAsLong();
		try {
			var obj = sqlSupp.get();
			err = false;
			return obj;
		}
		finally {
			var fin = currentTimeMillis();
			accept(new DatabaseAction(action, ofEpochMilli(beg), ofEpochMilli(fin), err));
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
