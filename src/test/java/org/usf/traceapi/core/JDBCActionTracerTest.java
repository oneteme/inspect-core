package org.usf.traceapi.core;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.isNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.usf.traceapi.core.JDBCActionTracer.SQLSupplier;

class JDBCActionTracerTest {
	
	static final String query = "dummy query";
	static final Instant[] period = new Instant[2];

	private JDBCActionTracer tracer;
	
	@BeforeEach
	void init() {
		tracer = new JDBCActionTracer();
	}

	@ParameterizedTest
	@ValueSource(ints={0, 5, 10, 20, 50, 100, 500, 1000})
	void testTrace(int milli) {
		var action = JDBCAction.values()[milli % JDBCAction.values().length];
		var act = assertDoesNotThrow(()-> tracer.trace(action, awaitAndReturn(milli, milli)));
		assertEquals(milli, act);
		assertAction(action, null, milli, null);
	}
	
	@ParameterizedTest
	@ValueSource(ints={0, 5, 10, 20, 50, 100, 500, 1000})
	void testTrace_fails(int milli) {
		var action = JDBCAction.values()[milli % JDBCAction.values().length];
		var ex = new SQLException("dummy msg");
		assertThrows(SQLException.class, ()-> tracer.trace(action, awaitAndThrow(milli, ex)));
		assertAction(action, null, milli, ex);
	}
	
	void assertAction(JDBCAction action, long[] count, int duration, SQLException ex){
		var tr = tracer.getActions().getLast();
		assertEquals(action, tr.getType());
		assertBetween(-1, 0, period[0].until(tr.getStart(), MILLIS), "start"); //delay=1
		assertBetween( 0, 1, period[1].until(tr.getEnd(), MILLIS), "end");	//delay=1
		assertBetween(duration, duration + 20, tr.duration(), "duration"); //delay=20
		assertArrayEquals(count, tr.getCount(), "count");
		if(isNull(ex)) {
			assertNull(tr.getException(), "exception");
		}
		else {
			assertEquals(ex.getMessage(), tr.getException().getMessage(), "exception.message");
			assertEquals(ex.getClass().getName(), tr.getException().getClassname(), "exception.classname");
		}
	}
	
	static <T> SQLSupplier<T> awaitAndThrow(int millis, SQLException ex){
		return awaitAndReturn(millis, ()-> {throw ex;});
	}

	static <T> SQLSupplier<T> awaitAndReturn(int millis, T obj){
		return awaitAndReturn(millis, ()-> obj);
	}

	static <T> SQLSupplier<T> awaitAndReturn(int millis, SQLSupplier<T> supp){
		return ()-> {
			try {
				period[0] = now();
				sleep(millis);
				return supp.get();
			} catch (InterruptedException e) {
				currentThread().interrupt();
				throw new AssertionError();
			}
			finally {
				period[1] = now();
			}
		};
	}
	
	static void assertBetween(long min, long max, long actual, String message) {
		assertTrue(actual >= min, message);
		assertTrue(actual <= max, message);
	}
}
