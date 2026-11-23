 package org.usf.inspect.sql;

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

	public CallableStatementWrapper(CallableStatement cs, ConnectionWrapper cnx, StatementLifecycleTracer tracer) {
		super(cs, cnx, tracer);
		this.cs = cs;
	}
}
