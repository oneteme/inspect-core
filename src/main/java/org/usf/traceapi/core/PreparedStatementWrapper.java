package org.usf.traceapi.core;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@SuppressWarnings("resource")
public final class PreparedStatementWrapper extends StatementWrapper implements PreparedStatement {
	
	public PreparedStatementWrapper(PreparedStatement st, DatabaseActionTracer tracer) {
		super(st, tracer);
	}

	@Override
	public boolean execute() throws SQLException {
		return tracer.sql(ps()::execute);
	}
	
	@Override
	public ResultSet executeQuery() throws SQLException {
		return tracer.select(ps()::executeQuery);
	}
	
	@Override
	public int executeUpdate() throws SQLException {
		return tracer.update(ps()::executeUpdate);
	}
	
	@Override
	public long executeLargeUpdate() throws SQLException {
		return tracer.update(ps()::executeLargeUpdate);
	}
	
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return tracer.resultSetMetadata(ps()::getMetaData);
	}
	
	@Delegate
	private PreparedStatement ps() {
		return (PreparedStatement) st;
	}
}
