package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static org.usf.traceapi.core.ApiTraceFilter.localTrace;

import java.sql.Connection;
import java.sql.SQLException;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public final class ConnectionTrace implements Connection {
	
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
			var end = currentTimeMillis();
			localTrace.get().getQueries().add(new MainQuery(start, end, failed));
		}
	}

}
