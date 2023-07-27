package org.usf.traceapi.core;

import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class ResultSetWrapper implements ResultSet {

	@Delegate
	private final ResultSet rs;
	private final DatabaseActionTracer tracer;
	private final long start;
	
	@Override
	public void close() throws SQLException {
		tracer.fetch(start, rs::close);
	}
	
}
