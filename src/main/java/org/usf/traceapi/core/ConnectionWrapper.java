package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static org.usf.traceapi.core.TraceConfiguration.localTrace;

import java.sql.Connection;
import java.sql.SQLException;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public final class ConnectionWrapper implements Connection {
	
	@Delegate
	private final Connection cn;
	private final long start;

	@Override
	public void close() throws SQLException {
		boolean failed = true;
		try {
			cn.close();
			failed = false;
		}
		finally {
			var trc = localTrace.get();
			if(trc != null) {
				var end = currentTimeMillis();
				trc.push(new OutcomingQuery(start, end, failed));
			}
		}
	}
	
}
