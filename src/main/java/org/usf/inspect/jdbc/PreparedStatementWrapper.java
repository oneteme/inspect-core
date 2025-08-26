package org.usf.inspect.jdbc;

import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.jdbc.JDBCAction.METADATA;

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
		exec(ps::addBatch, monitor.addBatchStageHandler(null));
	}
	
	@Override
	public boolean execute() throws SQLException {
		return call(ps::execute, monitor.executeStageHandler(null));
	}
	
	@Override
	public ResultSet executeQuery() throws SQLException {
		return new ResultSetWrapper(call(ps::executeQuery, monitor.executeQueryStageHandler(null)), monitor);
	}
	
	@Override
	public int executeUpdate() throws SQLException {
		return call(ps::executeUpdate, monitor.executeUpdateStageHandler(null));
	}
	
	@Override
	public long executeLargeUpdate() throws SQLException {
		return call(ps::executeLargeUpdate, monitor.executeLargeUpdateStageHandler(null));
	}
	
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return call(ps::getMetaData, monitor.stageHandler(METADATA));
	}
}
