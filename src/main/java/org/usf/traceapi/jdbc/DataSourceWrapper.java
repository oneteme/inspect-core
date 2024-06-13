package org.usf.traceapi.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.usf.traceapi.core.SafeCallable;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataSourceWrapper implements DataSource {
	
	@Delegate
	private final DataSource ds;

	@Override
	public Connection getConnection() throws SQLException {
		return getConnection(ds::getConnection);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection(()-> ds.getConnection(username, password));
	}
	
	private Connection getConnection(SafeCallable<Connection, SQLException> cnSupp) throws SQLException {
		return new JDBCActionTracer().connection(cnSupp);
	}
	
	public static final DataSourceWrapper wrap(DataSource ds) {
		return new DataSourceWrapper(ds);
	}
}
