package org.usf.inspect.sql;

import static java.time.Clock.systemUTC;
import static java.util.Objects.nonNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Wrapper;
import java.time.Instant;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
public final class ResultSetWrapper extends AbstractWrapper<ResultSet> implements ResultSet {

	@Delegate(excludes = Wrapper.class)
	protected final ResultSet rs; //used in statement
	private final ConnectionWrapper connection;
	private final StatementWrapper statement;
	private final Instant start = systemUTC().instant();
	private long rows;
	
	//TODO check if TYPE_FORWARD_ONLY then do not count rows, as it will be very expensive for large resultset
	public ResultSetWrapper(ResultSet rs, ConnectionWrapper connection, StatementWrapper statement) {
		super(rs);
		this.rs = rs;
		this.connection = connection;
		this.statement = statement; //nullable for ResultSet returned by DatabaseMetaData
	}

	@Override
	public boolean previous() throws SQLException {
		return updateRows(rs.previous()); //after Last, then previous() will return true and update rows count
	}

	@Override
	public boolean next() throws SQLException {
		return updateRows(rs.next(), 1);
	}
	
	@Override
	public boolean first() throws SQLException {
		return updateRows(rs.first());
	}

	@Override
	public boolean last() throws SQLException {
		return updateRows(rs.last());
	}
	
	@Override
	public boolean absolute(int row) throws SQLException {
		return updateRows(rs.absolute(row));
	}
	
	@Override
	public boolean relative(int rows) throws SQLException {
		return updateRows(rs.relative(rows));
	}
	
	@Override
	public void beforeFirst() throws SQLException {
		rs.beforeFirst(); //do nothing else
	}
	
	@Override
	public void afterLast() throws SQLException {
		rs.afterLast(); //do nothing else
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
	
	private boolean updateRows(boolean condition, int inc) {
		if(condition) {
			rows += inc;
			return true;
		}
		return false;
	}
	
	@Override
	public Statement getStatement() throws SQLException {
		return nonNull(statement) ? statement : rs.getStatement();
	}
	
	@Override
	public void close() throws SQLException {
		try {
			rs.close();
		}
		finally {
			connection.releaseWrapper(this);
		}
	}
	
	@Override
	protected void ensureTraced() {
		connection.getTracer().emitFetchStage(start, systemUTC().instant(), rows);
	}
}
