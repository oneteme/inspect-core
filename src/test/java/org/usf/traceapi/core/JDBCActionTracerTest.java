package org.usf.traceapi.core;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.usf.traceapi.core.JDBCAction.CONNECTION;
import static org.usf.traceapi.core.JDBCAction.STATEMENT;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.usf.traceapi.core.JDBCActionTracer.SQLSupplier;

class JDBCActionTracerTest {
	
	static final SQLException exception = new SQLException("dummy msg");

	private JDBCActionTracer tracer;
	
	@BeforeEach
	void init() {
		tracer = new JDBCActionTracer();
	}
	
	@ParameterizedTest
	@ValueSource(ints= {0, 5, 10, 20, 50})
	void testConnection(int milli) {
		Connection cnx = null;
		var act = assertAction(CONNECTION, null, false, ()-> tracer.connection(awaitAndReturn(milli, cnx)));
		assertEquals(cnx, act.getCn());
		assertEquals(tracer, act.getTracer());
	}
	
	@ParameterizedTest
	@ValueSource(ints= {0, 5, 10, 20, 50})
	void testConnection_fail(int milli) {
		assertAction(CONNECTION, null, true, ()-> tracer.connection(awaitAndThrow(milli)));
	}

	@ParameterizedTest
	@ValueSource(ints= {0, 5, 10, 20, 50})
	void testStatement(int milli) {
		Statement st = null;
		var act = assertAction(STATEMENT, null, false, ()-> tracer.statement(awaitAndReturn(milli, st)));
		assertEquals(st, act.getSt());
		assertEquals(tracer, act.getTracer());
	}
	
	@ParameterizedTest
	@ValueSource(ints= {0, 5, 10, 20, 50})
	void testStatement_fail(int milli) {
		assertAction(STATEMENT, null, true, ()-> tracer.statement(awaitAndThrow(milli)));
	}

	@ParameterizedTest
	@ValueSource(ints= {0, 5, 10, 20, 50})
	void testPreparedStatement(int milli) {
		PreparedStatement ps = null;
		var query = "dummy query";
		var act = assertAction(STATEMENT, null, false, ()-> tracer.preparedStatement(query, awaitAndReturn(milli, ps)));
		assertEquals(ps, act.getSt());
		assertEquals(tracer, act.getTracer());
		assertEquals(query, act.getSql());
	}
	

	@ParameterizedTest
	@ValueSource(ints= {0, 5, 10, 20, 50})
	void testPreparedStatement_fail(int milli) {
		assertAction(STATEMENT, null, true, ()-> tracer.preparedStatement("", awaitAndThrow(milli)));
	}

	<T> T assertAction(JDBCAction action, long[] count, boolean fail, SQLSupplier<T> supplier){
		var s = now();
		try {
			return supplier.get();
		}
		catch (Exception e) {
			assertEquals(exception, e);
		}
		finally {
			var e = now();
			var tr = tracer.getActions().getLast();
			assertEquals(action, tr.getType(), "type");
			assertTrue(s.compareTo(tr.getStart()) < 1, "start");
			assertTrue(e.compareTo(tr.getEnd()) > -1, "end");
			assertArrayEquals(count, tr.getCount(), "count");
			if(fail) {
				assertEquals(exception.getMessage(), tr.getException().getMessage(), "exception.message");
				assertEquals(exception.getClass().getName(), tr.getException().getClassname(), "exception.classname");
			}
			else {
				assertNull(tr.getException(), "exception");
			}
		}
		return null;
	}
	
	static <T> SQLSupplier<T> awaitAndThrow(int millis){
		return awaitAndReturn(millis, ()-> {throw exception;});
	}

	static <T> SQLSupplier<T> awaitAndReturn(int millis, T obj){
		return awaitAndReturn(millis, ()-> obj);
	}

	static <T> SQLSupplier<T> awaitAndReturn(int millis, SQLSupplier<T> supp){
		return ()-> {
			try {
				sleep(millis);
				return supp.get();
			} catch (InterruptedException e) {
				currentThread().interrupt();
				throw new AssertionError();
			}
		};
	}
}
