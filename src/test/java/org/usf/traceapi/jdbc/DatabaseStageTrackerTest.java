package org.usf.traceapi.jdbc;

import static org.usf.traceapi.jdbc.JdbcURLDecoder.decode;

import java.util.Arrays;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DatabaseStageTrackerTest {
	
	@ParameterizedTest
	@CsvSource(nullValues="", value={
		"jdbc:postgresql://localhost:5432/mydatabase,localhost,5432,mydatabase",
		"jdbc:postgresql://192.168.1.100:5432/sampledb,192.168.1.100,5432,sampledb",
		"jdbc:postgresql://db.example.com:5432/sampledb,db.example.com,5432,sampledb",
		"jdbc:postgresql://localhost/mydatabase?user=testuser&password=testpass,localhost,,mydatabase",
		"jdbc:postgresql://192.168.1.100/sampledb?ssl=true,192.168.1.100,,sampledb",
		"jdbc:postgresql://db.example.com/sampledb?charSet=UTF8,db.example.com,,sampledb",
		"jdbc:postgresql://localhost:5432/,localhost,5432,",
		"jdbc:postgresql://192.168.1.100:5432,192.168.1.100,5432,",
		"jdbc:postgresql://db.example.com:5432,db.example.com,5432,",
	})
	void testDecode_postgresql(String origin, String host, String port, String database) {
		
		var arr = decode(origin);
		System.out.println(origin + "\t" + Arrays.toString(arr));
		
//		assertEquals("postgresql", arr[0]);
//		assertEquals(host, arr[1]);
//		assertEquals(port, arr[2]);
//		assertEquals(database, arr[3]);
	}
	
	@ParameterizedTest
	@CsvSource(nullValues="", value={
		"jdbc:teradata://hostname:1025/database=mydatabase",
		"jdbc:teradata://192.168.1.100:5432/database=mydatabase",
		"jdbc:teradata://db.example.com:8000/database=mydatabase",
		"jdbc:teradata://hostname:1025/database=mydatabase,user=username,password=password",
		"jdbc:teradata://192.168.1.100:5432/database=mydatabase,user=testuser,password=testpass",
		"jdbc:teradata://db.example.com:8000/database=mydatabase,user=admin,password=secret",
		"jdbc:teradata://hostname:1025/",
		"jdbc:teradata://192.168.1.100:5432/",
		"jdbc:teradata://db.example.com:8000/",
	})
	void testDecode_teradata(String origin) {

		var arr = decode(origin);
		System.out.println(origin + "\t" + Arrays.toString(arr));
		
//		var arr = decode(origin);
//		assertEquals("postgresql", arr[0]);
//		assertEquals(host, arr[1]);
//		assertEquals(port, arr[2]);
//		assertEquals(database, arr[3]);
	}
	
	@ParameterizedTest
	@CsvSource(nullValues="", value={
		"jdbc:sqlserver://hostname:port;databaseName=mydatabase;encrypt=true;trustServerCertificate=true;",
		"jdbc:sqlserver://hostname:port;encrypt=true;trustServerCertificate=true;databaseName=mydatabase;",
		"jdbc:sqlserver://hostname:port;encrypt=true;databaseName=mydatabase;trustServerCertificate=true;",
	})
	void testDecode_sqlServer(String origin) {

		var arr = decode(origin);
		System.out.println(origin + "\t" + Arrays.toString(arr));
		
//		var arr = decode(origin);
//		assertEquals("postgresql", arr[0]);
//		assertEquals(host, arr[1]);
//		assertEquals(port, arr[2]);
//		assertEquals(database, arr[3]);
	}
	
	@ParameterizedTest
	@CsvSource(nullValues="", value={
		"jdbc:h2:mem:testdb",
		"jdbc:h2:file:/path/to/database",
	})
	void testDecode_h2(String origin) {

		var arr = decode(origin);
		System.out.println(origin + "\t" + Arrays.toString(arr));
		
//		var arr = decode(origin);
//		assertEquals("postgresql", arr[0]);
//		assertEquals(host, arr[1]);
//		assertEquals(port, arr[2]);
//		assertEquals(database, arr[3]);
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
