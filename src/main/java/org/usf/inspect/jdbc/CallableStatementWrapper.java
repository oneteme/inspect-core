 package org.usf.inspect.jdbc;

import java.sql.CallableStatement;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
public class CallableStatementWrapper extends PreparedStatementWrapper implements CallableStatement {
	
	@Delegate
	private final CallableStatement cs;

	public CallableStatementWrapper(CallableStatement cs, DatabaseConnectionLifecycleTracer tracer) {
		super(cs, tracer);
		this.cs = cs;
	}
}
