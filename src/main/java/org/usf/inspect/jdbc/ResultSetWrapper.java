package org.usf.inspect.jdbc;

import static java.time.Instant.now;
import static org.usf.inspect.core.ExecutionMonitor.exec;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

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
	private final DatabaseRequestMonitor monitor;
	private final Instant start = now();
	private int rows;

	@Override
	public boolean previous() throws SQLException {
		return updateRows(rs.previous());
	}
	
	@Override
	public boolean first() throws SQLException {
		return updateRows(rs.first());
	}
	
	@Override
	public void beforeFirst() throws SQLException {
		rs.beforeFirst(); //do nothing else
	}

	@Override
	public boolean next() throws SQLException {
		return updateRows(rs.next());
	}

	@Override
	public boolean last() throws SQLException {
		return updateRows(rs.last());
	}
	
	@Override
	public void afterLast() throws SQLException {
		rs.afterLast(); //do nothing else
	}
	
	@Override
	public boolean absolute(int row) throws SQLException {
		return updateRows(rs.absolute(row));
	}
	
	@Override
	public boolean relative(int rows) throws SQLException {
		return updateRows(rs.relative(rows));
	}
	
	private boolean updateRows(boolean condition) throws SQLException {
		if(condition) {
			var row = rs.getRow();
			if(row > rows){
				rows = row;
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void close() throws SQLException {
		exec(rs::close, monitor.fetch(start, rows));
	}
}
