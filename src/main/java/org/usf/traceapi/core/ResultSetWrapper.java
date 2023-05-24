package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;

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

	public ResultSetWrapper(ResultSet resultSet, DatabaseActionTracer wrapper) {
		this(resultSet, wrapper, currentTimeMillis());
	}

	@Override
	public void close() throws SQLException {
		wrapper.fetch(start, resultSet::close);
	}
}
