package org.usf.inspect.sql;

import static org.usf.inspect.DatabaseAction.METADATA;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.InspectExecutor.exec;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Wrapper;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
public class PreparedStatementWrapper extends StatementWrapper implements PreparedStatement {

	@Delegate(excludes = {Wrapper.class, Statement.class})
	private final PreparedStatement ps;

	public PreparedStatementWrapper(PreparedStatement ps, ConnectionWrapper cnx, StatementLifecycleTracer tracer) {
		super(ps, cnx, tracer);
		this.ps = ps;   // duplicated reference, cannot delegate in super class 
	}

	@Override
	public void addBatch() throws SQLException {
		exec(ps::addBatch, tracer.addBatchStageListener(null));
	}
	
	@Override
	public boolean execute() throws SQLException {
		return releaseAndExecute(ps::execute, tracer.executeStageListener(null));
	}
	
	@Override
	public int executeUpdate() throws SQLException {
		return releaseAndExecute(ps::executeUpdate, tracer.executeUpdateStageListener(null));
	}
	
	@Override
	public long executeLargeUpdate() throws SQLException {
		return releaseAndExecute(ps::executeLargeUpdate, tracer.executeUpdateStageListener(null));
	}
	
	@Override
	public ResultSet executeQuery() throws SQLException {
		return releaseAndExecuteQuery(ps::executeQuery, tracer.executeStageListener(null));
	}
	
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return call(ps::getMetaData, tracer.getConnectionTracer().stageListener(METADATA, null));
	}
}
