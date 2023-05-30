package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
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
		var req = localTrace.get();
		if(nonNull(req)) {
			var oc = new OutcomingQuery();
			req.append(oc);
			DatabaseActionTracer tracer = oc::append;
			oc.setStart(ofEpochMilli(currentTimeMillis()));
			var cn = tracer.connection(cnSupp); //differed end
			cn.setOnClose(()-> oc.setEnd(ofEpochMilli(currentTimeMillis())));
			return cn;
		}
		return cnSupp.get();
	}
	
}
