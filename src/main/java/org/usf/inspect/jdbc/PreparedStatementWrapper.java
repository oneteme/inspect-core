package org.usf.inspect.jdbc;

import static org.usf.inspect.core.DatabaseAction.METADATA;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.InspectExecutor.exec;

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
public class PreparedStatementWrapper extends StatementWrapper implements PreparedStatement {

	@Delegate(excludes = Statement.class)
	private final PreparedStatement ps;

	public PreparedStatementWrapper(PreparedStatement ps, DatabaseRequestListener tracer) {
		super(ps, tracer);
		this.ps = ps;
	}

	@Override
	public void addBatch() throws SQLException {
		exec(ps::addBatch, monitor.addBatchStageListener(null));
	}
	
	@Override
	public boolean execute() throws SQLException {
		return call(ps::execute, monitor.executeStageListener(null));
	}
	
	@Override
	public ResultSet executeQuery() throws SQLException {
		return new ResultSetWrapper(call(ps::executeQuery, monitor.executeQueryStageListener(null)), monitor);
	}
	
	@Override
	public int executeUpdate() throws SQLException {
		return call(ps::executeUpdate, monitor.executeUpdateStageListener(null));
	}
	
	@Override
	public long executeLargeUpdate() throws SQLException {
		return call(ps::executeLargeUpdate, monitor.executeLargeUpdateStageListener(null));
	}
	
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return call(ps::getMetaData, monitor.stageListener(METADATA));
	}
}
