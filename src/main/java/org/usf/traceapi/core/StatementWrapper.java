package org.usf.traceapi.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@Getter(AccessLevel.PACKAGE)
@RequiredArgsConstructor
public class StatementWrapper implements Statement {

	@Delegate
	protected final Statement st;
	protected final JDBCActionTracer tracer;
	
	@Override
	public void addBatch(String sql) throws SQLException {
		tracer.addBatch(sql, ()-> st.addBatch(sql));
	}
	
	@Override
	public boolean execute(String sql) throws SQLException {
		return tracer.execute(sql, ()-> st.execute(sql));
	}
	
	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		return tracer.execute(sql, ()-> st.execute(sql, autoGeneratedKeys));
	}
	
	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return tracer.execute(sql, ()-> st.execute(sql, columnIndexes));
	}
	
	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		return tracer.execute(sql, ()-> st.execute(sql, columnNames));
	}
	
	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		return tracer.executeQuery(sql, ()-> st.executeQuery(sql));
	}
	
	@Override
	public int executeUpdate(String sql) throws SQLException {
		return tracer.executeUpdate(sql, ()-> st.executeUpdate(sql));
	}
	
	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		return tracer.executeUpdate(sql, ()-> st.executeUpdate(sql, autoGeneratedKeys));
	}
	
	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		return tracer.executeUpdate(sql, ()-> st.executeUpdate(sql, columnIndexes));
	}
	
	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		return tracer.executeUpdate(sql, ()-> st.executeUpdate(sql, columnNames));
	}

	@Override
	public int[] executeBatch() throws SQLException {
		return tracer.executeBatch(null, st::executeBatch);
	}
	
	@Override
	public long executeLargeUpdate(String sql) throws SQLException {
		return tracer.executeLargeUpdate(sql, ()-> st.executeLargeUpdate(sql));
	}
	
	@Override
	public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		return tracer.executeLargeUpdate(sql, ()-> st.executeLargeUpdate(sql, autoGeneratedKeys));
	}
	
	@Override
	public long executeLargeUpdate(String sql, int[] columnIndexes) throws SQLException {
		return tracer.executeLargeUpdate(sql, ()-> st.executeLargeUpdate(sql, columnIndexes));
	}
	
	@Override
	public long executeLargeUpdate(String sql, String[] columnNames) throws SQLException {
		return tracer.executeLargeUpdate(sql, ()-> st.executeLargeUpdate(sql, columnNames));
	}
	
	@Override
	public long[] executeLargeBatch() throws SQLException {
		return tracer.executeLargeBatch(null, st::executeLargeBatch);
	}
	
	@Override
	public ResultSet getResultSet() throws SQLException {
		return tracer.resultSet(st::getResultSet);
	}
	
	@Override
	public boolean getMoreResults() throws SQLException {
		return tracer.moreResults(this, st::getMoreResults);
	}
	
	@Override
	public boolean getMoreResults(int current) throws SQLException {
		return tracer.moreResults(this, ()-> st.getMoreResults(current));
	}
}
