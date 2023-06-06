package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.usf.traceapi.core.TraceConfiguration.localTrace;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
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

	private static final Pattern hostPattern =
			compile("^jdbc[:\\w+]+@?//([\\w+-\\.:]+)/(?:.*database=(\\w+).*|(\\w+)(?:\\?.*)?|.*)$", CASE_INSENSITIVE);
	
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
			out.setStart(ofEpochMilli(currentTimeMillis()));
			var cn = tracer.connection(cnSupp);
			out.setUrl(shortURL(cn.getMetaData().getURL()));
			out.setThread(currentThread().getName());
			cn.setOnClose(()-> out.setEnd(ofEpochMilli(currentTimeMillis()))); //differed end
			return cn;
		}
		return cnSupp.get();
	}
	
	static String shortURL(String url) {
		var m = hostPattern.matcher(url);
		return m.find() ? range(1, m.groupCount()+1).mapToObj(m::group).filter(Objects::nonNull).collect(joining("/")) : null;
	}
	
}
