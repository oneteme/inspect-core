package org.usf.traceapi.jdbc;

import static org.usf.traceapi.jdbc.JDBCActionTracer.connect;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class DataSourceWrapper implements DataSource {
	
	@Delegate
	private final DataSource ds;

	@Override
	public Connection getConnection() throws SQLException {
		return connect(ds::getConnection);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return connect(()-> ds.getConnection(username, password));
	}
}
