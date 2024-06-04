package org.usf.traceapi.jdbc;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.stackTraceElement;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.Helper.warnNoActiveSession;
import static org.usf.traceapi.core.StageTracker.call;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.usf.traceapi.core.DatabaseRequest;
import org.usf.traceapi.core.SafeCallable;

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
	private static final Pattern dbPattern = compile("database=(\\w+)", CASE_INSENSITIVE);
	
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
		var session = localTrace.get();
		if(isNull(session)) {
			warnNoActiveSession();
			return cnSupp.call();
		}
		JDBCActionTracer tracer = new JDBCActionTracer();
		return call(()-> tracer.connection(cnSupp), (s,e,cn,t)->{
			var out = new DatabaseRequest();
			out.setStart(s);
			out.setThreadName(threadName());
			out.setActions(tracer.getActions());
			out.setCommands(tracer.getCommands());
			stackTraceElement().ifPresent(st->{
				out.setName(st.getMethodName());
				out.setLocation(st.getClassName());
			});
			if(nonNull(cn)) {
				var meta = cn.getMetaData();
				var args = decodeURL(meta.getURL());
				out.setHost(args[0]);
				out.setPort(ofNullable(args[1]).map(Integer::parseInt).orElse(-1));
				out.setDatabase(args[2]);
				out.setUser(meta.getUserName());
				out.setDatabaseName(meta.getDatabaseProductName());
				out.setDatabaseVersion(meta.getDatabaseProductVersion());
				out.setDriverVersion(meta.getDriverVersion());
				cn.setOnClose(()-> out.setEnd(now())); //differed end
				//do not setException, already set in action
			}
			else {
				out.setEnd(e);
			}
			session.append(out);
		});
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
			m = dbPattern.matcher(url);
			if(m.find()) {
				arr[2] = m.group(1);
			}
		}
		return arr;
	}
}
