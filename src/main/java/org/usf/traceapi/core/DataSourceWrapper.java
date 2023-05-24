package org.usf.traceapi.core;

import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.TraceConfiguration.localTrace;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.usf.traceapi.core.DatabaseActionTracer.SQLSupplier;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
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
	
	private Connection getConnection(SQLSupplier<Connection> cnSupp) throws SQLException {
		var ir = localTrace.get();
		if(nonNull(ir)) {
			var oc = new OutcomingQuery();
			ir.push(oc);
			DatabaseActionTracer tracer = oc.getActions()::add;
			return tracer.connection(cnSupp);
		}
		return cnSupp.get();
	}
	
}
