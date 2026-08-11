 package org.usf.inspect.jdbc;

import java.sql.CallableStatement;

import lombok.experimental.Delegate;

/**
 * Wraps a {@link CallableStatement} to trace stored procedure execution.
 *
 * @author u$f
 */
public class CallableStatementWrapper extends PreparedStatementWrapper implements CallableStatement {
	
	@Delegate
	private final CallableStatement cs;

	/**
	 * Creates a wrapper around the given callable statement.
	 *
	 * @param cs the callable statement to wrap
	 * @param monitor the monitor used to trace statement activity
	 */
	public CallableStatementWrapper(CallableStatement cs, DatabaseRequestMonitor monitor) {
		super(cs, monitor);
		this.cs = cs;
	}
}
