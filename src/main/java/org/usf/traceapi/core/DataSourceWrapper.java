package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.threadName;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.usf.traceapi.core.DatabaseActionTracer.SQLSupplier;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class DataSourceWrapper implements DataSource {
	
	private static final Pattern hostPattern = compile("^jdbc:[\\w:]+@?//([-\\w\\.]+)(:(\\d+))?(/(\\w+)|/(\\w+)[\\?,;].*|.*)$", CASE_INSENSITIVE);
	private static final Pattern schemaPattern = compile("database=(\\w+)", CASE_INSENSITIVE);
	
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
			var out = new OutcomingQuery();
			req.append(out);
			DatabaseActionTracer tracer = out::append;
			out.setThreadName(threadName());
			out.setStart(ofEpochMilli(currentTimeMillis()));
			try {
				var cn = tracer.connection(cnSupp);
				var meta = cn.getMetaData();
				var args = decodeURL(meta.getURL());
				out.setHost(args[0]);
				out.setPort(ofNullable(args[1]).map(Integer::parseInt).orElse(null));
				out.setSchema(args[2]);
				out.setUser(meta.getUserName());
				out.setDatabaseName(meta.getDatabaseProductName());
				out.setDatabaseVersion(meta.getDatabaseProductVersion());
				out.setDriverVersion(meta.getDriverVersion());
				cn.setOnClose(()-> out.setEnd(ofEpochMilli(currentTimeMillis()))); //differed end
				return cn;
			}
			catch(SQLException e) {
				out.setEnd(ofEpochMilli(currentTimeMillis()));
			}
		}
		return cnSupp.get();
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
			m = schemaPattern.matcher(url);
			if(m.find()) {
				arr[2] = m.group(1);
			}
		}
		return arr;
	}
	
}
