package org.usf.inspect.jdbc;

import static java.time.Clock.systemUTC;
import static org.usf.inspect.core.InspectExecutor.exec;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * Wraps a {@link ResultSet} to trace row fetching activity.
 *
 * @author u$f
 */
@RequiredArgsConstructor
public final class ResultSetWrapper implements ResultSet {

	@Delegate
	private final ResultSet rs;
	private final DatabaseRequestMonitor monitor;
	private final Instant start = systemUTC().instant();
	private int rows;

	/**
	 * Moves the cursor to the previous row and updates the fetched row count.
	 *
	 * @return {@code true} if the cursor is positioned on a valid row
	 * @throws SQLException if the cursor cannot be moved
	 */
	@Override
	public boolean previous() throws SQLException {
		return updateRows(rs.previous());
	}
	
	/**
	 * Moves the cursor to the first row and updates the fetched row count.
	 *
	 * @return {@code true} if the cursor is positioned on a valid row
	 * @throws SQLException if the cursor cannot be moved
	 */
	@Override
	public boolean first() throws SQLException {
		return updateRows(rs.first());
	}
	
	/**
	 * Moves the cursor to the position before the first row.
	 *
	 * @throws SQLException if the cursor cannot be moved
	 */
	@Override
	public void beforeFirst() throws SQLException {
		rs.beforeFirst(); //do nothing else
	}

	/**
	 * Moves the cursor to the next row and updates the fetched row count.
	 *
	 * @return {@code true} if the cursor is positioned on a valid row
	 * @throws SQLException if the cursor cannot be moved
	 */
	@Override
	public boolean next() throws SQLException {
		return updateRows(rs.next());
	}

	/**
	 * Moves the cursor to the last row and updates the fetched row count.
	 *
	 * @return {@code true} if the cursor is positioned on a valid row
	 * @throws SQLException if the cursor cannot be moved
	 */
	@Override
	public boolean last() throws SQLException {
		return updateRows(rs.last());
	}
	
	/**
	 * Moves the cursor to the position after the last row.
	 *
	 * @throws SQLException if the cursor cannot be moved
	 */
	@Override
	public void afterLast() throws SQLException {
		rs.afterLast(); //do nothing else
	}
	
	/**
	 * Moves the cursor to the specified row and updates the fetched row count.
	 *
	 * @param row the target row number
	 * @return {@code true} if the cursor is positioned on a valid row
	 * @throws SQLException if the cursor cannot be moved
	 */
	@Override
	public boolean absolute(int row) throws SQLException {
		return updateRows(rs.absolute(row));
	}
	
	/**
	 * Moves the cursor by the specified number of rows and updates the fetched row count.
	 *
	 * @param rows the number of rows to move relative to the current row
	 * @return {@code true} if the cursor is positioned on a valid row
	 * @throws SQLException if the cursor cannot be moved
	 */
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
	
	/**
	 * Closes the wrapped result set and reports the fetch metrics.
	 *
	 * @throws SQLException if the result set cannot be closed
	 */
	@Override
	public void close() throws SQLException {
		exec(rs::close, monitor.fetch(start, rows));
	}
}
