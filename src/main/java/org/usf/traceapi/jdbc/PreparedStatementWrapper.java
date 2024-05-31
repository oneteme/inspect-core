package org.usf.traceapi.jdbc;

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
	private final String sql;

	public PreparedStatementWrapper(PreparedStatement ps, JDBCActionTracer tracer, String sql) {
		super(ps, tracer);
		this.ps = ps;
		this.sql = sql;
	}

	@Override
	public void addBatch() throws SQLException {
		tracer.addBatch(null, ps::addBatch);
	}
	
	@Override
	public boolean execute() throws SQLException {
		return tracer.execute(sql, ps::execute);
	}
	
	@Override
	public ResultSet executeQuery() throws SQLException {
		return tracer.executeQuery(sql, ps::executeQuery);
	}
	
	@Override
	public int executeUpdate() throws SQLException {
		return tracer.executeUpdate(sql, ps::executeUpdate);
	}
	
	@Override
	public long executeLargeUpdate() throws SQLException {
		return tracer.executeLargeUpdate(sql, ps::executeLargeUpdate);
	}
	
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return tracer.resultSetMetadata(ps::getMetaData);
	}
	
	// Override StatementWrapper

	@Override
	public int[] executeBatch() throws SQLException {
		return tracer.executeBatch(sql, st::executeBatch);
	}
	
	@Override
	public long[] executeLargeBatch() throws SQLException {
		return tracer.executeLargeBatch(sql, st::executeLargeBatch);
	}
}
