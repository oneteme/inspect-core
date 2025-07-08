package org.usf.inspect.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
public final class PreparedStatementWrapper extends StatementWrapper implements PreparedStatement {

	@Delegate(excludes = Statement.class)
	private final PreparedStatement ps;

	public PreparedStatementWrapper(PreparedStatement ps, DatabaseRequestMonitor tracer) {
		super(ps, tracer);
		this.ps = ps;
	}

	@Override
	public void addBatch() throws SQLException {
		tracer.addBatch(null, ps::addBatch);
	}
	
	@Override
	public boolean execute() throws SQLException {
		return tracer.execute(null, ps::execute, ps);
	}
	
	@Override
	public ResultSet executeQuery() throws SQLException {
		return tracer.executeQuery(null, ps::executeQuery);
	}
	
	@Override
	public int executeUpdate() throws SQLException {
		return tracer.executeUpdate(null, ps::executeUpdate);
	}
	
	@Override
	public long executeLargeUpdate() throws SQLException {
		return tracer.executeLargeUpdate(null, ps::executeLargeUpdate);
	}
	
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return tracer.resultSetMetadata(ps::getMetaData);
	}
}
