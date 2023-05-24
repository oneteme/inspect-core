package org.usf.traceapi.core;

import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public final class ResultSetWrapper implements ResultSet {

	@Delegate
	private final ResultSet resultSet;
	private final DatabaseActionTracer wrapper;
	private final long start;

	@Override
	public void close() throws SQLException {
		wrapper.fetch(start, resultSet::close);
	}
}
