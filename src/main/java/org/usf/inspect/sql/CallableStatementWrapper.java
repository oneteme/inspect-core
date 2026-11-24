package org.usf.inspect.sql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Wrapper;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
public class CallableStatementWrapper extends PreparedStatementWrapper implements CallableStatement {
	
	@Delegate(excludes = {Wrapper.class, Statement.class, PreparedStatement.class})
	private final CallableStatement cs;

	public CallableStatementWrapper(CallableStatement cs, ConnectionWrapper cnx, StatementLifecycleTracer tracer) {
		super(cs, cnx, tracer);
		this.cs = cs; // duplicated reference, cannot delegate in super class 
	}
}
