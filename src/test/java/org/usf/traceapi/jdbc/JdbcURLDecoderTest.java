package org.usf.traceapi.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.traceapi.jdbc.JdbcURLDecoder.decode;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JdbcURLDecoderTest {
	
	@ParameterizedTest
	@CsvSource(nullValues="", value={
		"jdbc:postgresql://localhost:5432/mydatabase,postgresql,localhost,5432,mydatabase",
		"jdbc:postgresql://192.168.1.100:5432/sampledb,postgresql,192.168.1.100,5432,sampledb",
		"jdbc:postgresql://db.example.com:5432/sampledb,postgresql,db.example.com,5432,sampledb",
		"jdbc:postgresql://localhost/mydatabase?user=testuser&password=testpass,postgresql,localhost,,mydatabase",
		"jdbc:postgresql://192.168.1.100/sampledb?ssl=true,postgresql,192.168.1.100,,sampledb",
		"jdbc:postgresql://db.example.com/sampledb?charSet=UTF8,postgresql,db.example.com,,sampledb",
		"jdbc:postgresql://localhost:5432/,postgresql,localhost,5432,",
		"jdbc:postgresql://192.168.1.100:5432,postgresql,192.168.1.100,5432,",
		"jdbc:postgresql://db.example.com:5432,postgresql,db.example.com,5432,",
	})
	void testDecode_postgresql(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}
	
	@ParameterizedTest
	@CsvSource(nullValues="", delimiter = ';', value={
		"jdbc:teradata://hostname:1025/database=mydatabase;teradata;hostname;1025;mydatabase",
		"jdbc:teradata://192.168.1.100:5432/database=mydatabase;teradata;192.168.1.100;5432;mydatabase",
		"jdbc:teradata://db.example.com:8000/database=mydatabase;teradata;db.example.com;8000;mydatabase",
		"jdbc:teradata://hostname:1025/database=mydatabase,user=username,password=password;teradata;hostname;1025;mydatabase",
		"jdbc:teradata://192.168.1.100:5432/database=mydatabase,user=testuser,password=testpass;teradata;192.168.1.100;5432;mydatabase",
		"jdbc:teradata://db.example.com:8000/database=mydatabase,user=admin,password=secret;teradata;db.example.com;8000;mydatabase",
		"jdbc:teradata://hostname:1025/;teradata;hostname;1025;",
		"jdbc:teradata://192.168.1.100:5432/;teradata;192.168.1.100;5432;",
		"jdbc:teradata://db.example.com:8000/;teradata;db.example.com;8000;",
	})
	void testDecode_teradata(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}
	
	@ParameterizedTest
	@CsvSource(nullValues="", value={
		"jdbc:sqlserver://hostname:1234;databaseName=mydatabase;encrypt=true;trustServerCertificate=true;,sqlserver,hostname,1234,mydatabase",
		"jdbc:sqlserver://hostname:1234;encrypt=true;trustServerCertificate=true;databaseName=mydatabase;,sqlserver,hostname,1234,mydatabase",
		"jdbc:sqlserver://hostname:1234;encrypt=true;databaseName=mydatabase;trustServerCertificate=true;,sqlserver,hostname,1234,mydatabase",
	})
	void testDecode_sqlServer(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}
	
	@ParameterizedTest
	@CsvSource(nullValues="", value={
		"jdbc:h2:mem:testdb,h2,,,testdb",
		"jdbc:h2:file:/path/to/database,h2,,,/path/to/database",
	})
	void testDecode_h2(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}
	
	private static void assertURLDecode(String origin, String db, String host, String port, String name) {
		var arr = decode(origin);
		assertEquals(db, arr[0]);
		assertEquals(host, arr[1]);
		assertEquals(port, arr[2]);
		assertEquals(name, arr[3]);
	}
}
