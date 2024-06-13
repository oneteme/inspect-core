package org.usf.traceapi.jdbc;

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
import static org.usf.traceapi.jdbc.JDBCActionTracer.decodeURL;

import java.sql.SQLException;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class JDBCActionTracerTest {
	
	@ParameterizedTest
	@CsvSource(nullValues = {""}, value={
			"jdbc:teradata://TDHOST/CHARSET=UTF8;DATABASE=my_data;SLOB_RECEIVE_THRESHOLD=15000,TDHOST,,my_data",
			"jdbc:teradata://TDHOST:666/database=my_data;CHARSET=UTF8;SLOB_RECEIVE_THRESHOLD=15000,TDHOST,666,my_data",
			"jdbc:teradata://TDHOST/CHARSET=UTF8,TDHOST,,",
			"jdbc:oracle:thin:@//myoracle.db.server:1521/my_servicename,myoracle.db.server,1521,my_servicename",
			"jdbc:mysql://mysql.db.server:3306/my_database?useSSL=false&serverTimezone=UTC,mysql.db.server,3306,my_database",
			"jdbc:postgresql//10.123.321.44:4455/db,10.123.321.44,4455,db"})
	void testShortURL(String origin, String host, String port, String schema) {
		var arr = decodeURL(origin);
		assertEquals(host, arr[0]);
		assertEquals(port, arr[1]);
		assertEquals(schema, arr[2]);
	}
	
//	static final String query = "dummy query";
//	static final Instant[] period = new Instant[2];
//
//	private JDBCActionTracer tracer;
//	
//	@BeforeEach
//	void init() {
//		tracer = new JDBCActionTracer();
//	}
//
//	@ParameterizedTest
//	@ValueSource(ints={0, 5, 10, 20, 50, 100, 500, 1000})
//	void testTrace(int milli) {
//		var action = JDBCAction.values()[milli % JDBCAction.values().length];
//		var act = assertDoesNotThrow(()-> tracer.trace(action, awaitAndReturn(milli, milli)));
//		assertEquals(milli, act);
//		assertAction(action, null, milli, null);
//	}
//	
//	@ParameterizedTest
//	@ValueSource(ints={0, 5, 10, 20, 50, 100, 500, 1000})
//	void testTrace_fails(int milli) {
//		var action = JDBCAction.values()[milli % JDBCAction.values().length];
//		var ex = new SQLException("dummy msg");
//		assertThrows(SQLException.class, ()-> tracer.trace(action, awaitAndThrow(milli, ex)));
//		assertAction(action, null, milli, ex);
//	}
//	
//	void assertAction(JDBCAction action, long[] count, int duration, SQLException ex){
//		var tr = tracer.getActions().getLast();
//		assertEquals(action, tr.getType());
//		assertBetween(-1, 0, period[0].until(tr.getStart(), MILLIS), "start"); //delay=1
//		assertBetween( 0, 1, period[1].until(tr.getEnd(), MILLIS), "end");	//delay=1
//		assertBetween(duration, duration + 20, tr.duration(), "duration"); //delay=20
//		assertArrayEquals(count, tr.getCount(), "count");
//		if(isNull(ex)) {
//			assertNull(tr.getException(), "exception");
//		}
//		else {
//			assertEquals(ex.getMessage(), tr.getException().getMessage(), "exception.message");
//			assertEquals(ex.getClass().getName(), tr.getException().getClassname(), "exception.classname");
//		}
//	}
//	
//	static <T> SafeSupplier<T, SQLException> awaitAndThrow(int millis, SQLException ex){
//		return awaitAndReturn(millis, ()-> {throw ex;});
//	}
//
//	static <T> SafeSupplier<T, SQLException> awaitAndReturn(int millis, T obj){
//		return awaitAndReturn(millis, ()-> obj);
//	}
//
//	static <T> SafeSupplier<T, SQLException> awaitAndReturn(int millis, SafeSupplier<T, SQLException> supp){
//		return ()-> {
//			try {
//				period[0] = now();
//				sleep(millis);
//				return supp.get();
//			} catch (InterruptedException e) {
//				currentThread().interrupt();
//				throw new AssertionError();
//			}
//			finally {
//				period[1] = now();
//			}
//		};
//	}
//	
//	static void assertBetween(long min, long max, long actual, String message) {
//		assertTrue(actual >= min, message);
//		assertTrue(actual <= max, message);
//	}
}
