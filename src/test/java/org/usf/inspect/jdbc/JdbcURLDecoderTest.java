package org.usf.inspect.jdbc;

import static java.util.Objects.isNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.jdbc.JdbcURLDecoder.decode;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JdbcURLDecoderTest {
	
	@MethodSource("providePostgresUrls")
	@ParameterizedTest
	void testDecode_postgresql(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}

	@MethodSource("providePostgresUrls")
	@ParameterizedTest
	void testDecode_postgresql_upper(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin.toUpperCase(), db, uppercase(host), port, uppercase(name));
	}

	@MethodSource("provideMysqlFamilyUrls")
	@ParameterizedTest
	void testDecode_mysql_family(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}

	@MethodSource("provideMysqlFamilyUrls")
	@ParameterizedTest
	void testDecode_mysql_family_upper(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin.toUpperCase(), db, uppercase(host), port, uppercase(name));
	}

	@MethodSource("provideTeradataUrls")
	@ParameterizedTest
	void testDecode_teradata(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}

	@MethodSource("provideTeradataUrls")
	@ParameterizedTest
	void testDecode_teradata_upper(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin.toUpperCase(), db, uppercase(host), port, uppercase(name));
	}

	@MethodSource("provideSqlServerUrls")
	@ParameterizedTest
	void testDecode_sqlserver(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}

	@MethodSource("provideSqlServerUrls")
	@ParameterizedTest
	void testDecode_sqlserver_upper(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin.toUpperCase(), db, uppercase(host), port, uppercase(name));
	}

	@MethodSource("provideOracleUrls")
	@ParameterizedTest
	void testDecode_oracle(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}

	@MethodSource("provideOracleUrls")
	@ParameterizedTest
	void testDecode_oracle_upper(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin.toUpperCase(), db, uppercase(host), port, uppercase(name));
	}

	@MethodSource("provideClickhouseHttpUrls")
	@ParameterizedTest
	void testDecode_clickhouse_http(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}
	
	@MethodSource("provideClickhouseHttpUrls")
	@ParameterizedTest
	void testDecode_clickhouse_http_upper(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin.toUpperCase(), db, uppercase(host), port, uppercase(name));
	}

	@MethodSource("provideDatabricksUrls")
	@ParameterizedTest
	void testDecode_databricks(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}

	@MethodSource("provideDatabricksUrls")
	@ParameterizedTest
	void testDecode_databricks_upper(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin.toUpperCase(), db, uppercase(host), port, uppercase(name));
	}

	@MethodSource("provideH2Urls")
	@ParameterizedTest
	void testDecode_h2(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}

	@MethodSource("provideH2Urls")
	@ParameterizedTest
	void testDecode_h2_upper(String origin, String db, String host, String port, String name) {
		var expectedHost = "mem".equals(host) ? host : uppercase(host);
		assertURLDecode(origin.toUpperCase(), db, expectedHost, port, uppercase(name));
	}

	@MethodSource("provideFallbackUrls")
	@ParameterizedTest
	void testDecode_fallback(String origin, String db, String host, String port, String name) {
		assertURLDecode(origin, db, host, port, name);
	}
	
	static Stream<Arguments> providePostgresUrls() {
		return Stream.of(
			Arguments.of("jdbc:postgresql://localhost:5432/mydatabase", "postgresql", "localhost", "5432", "mydatabase"),
			Arguments.of("jdbc:postgresql://10.100.250.44:5432/mydatabase", "postgresql", "10.100.250.44", "5432", "mydatabase"),
			Arguments.of("jdbc:postgresql://db.example.com/mydatabase?ssl=true", "postgresql", "db.example.com", null, "mydatabase"),
			Arguments.of("jdbc:postgresql://localhost:5432/", "postgresql", "localhost", "5432", null),
			Arguments.of("jdbc:postgresql://localhost/mydatabase?user=testuser&password=testpass", "postgresql", "localhost", null, "mydatabase")
		);
	}

	static Stream<Arguments> provideMysqlFamilyUrls() {
		return Stream.of(
			Arguments.of("jdbc:mysql://localhost:3306/mydatabase", "mysql", "localhost", "3306", "mydatabase"),
			Arguments.of("jdbc:mysql://192.168.1.100/mydatabase?useSSL=false", "mysql", "192.168.1.100", null, "mydatabase"),
			Arguments.of("jdbc:mariadb://db.example.com:3307/sample", "mariadb", "db.example.com", "3307", "sample"),
			Arguments.of("jdbc:db2://db2.example.com:50000/sampledb", "db2", "db2.example.com", "50000", "sampledb"),
			Arguments.of("jdbc:mysql://host1:3306,host2:3306/prod", "mysql", "host1", "3306", "prod")
		);
	}

	static Stream<Arguments> provideTeradataUrls() {
		return Stream.of(
			Arguments.of("jdbc:teradata://hostname:1025/database=mydatabase", "teradata", "hostname", "1025", "mydatabase"),
			Arguments.of("jdbc:teradata://192.168.1.100:5432/database=mydatabase", "teradata", "192.168.1.100", "5432", "mydatabase"),
			Arguments.of("jdbc:teradata://db.example.com:8000/user=admin,database=mydatabase,password=secret", "teradata", "db.example.com", "8000", "mydatabase"),
			Arguments.of("jdbc:teradata://hostname/database=mydatabase", "teradata", "hostname", null, "mydatabase"),
			Arguments.of("jdbc:teradata://hostname:1025/", "teradata", "hostname", "1025", null)
		);
	}

	static Stream<Arguments> provideSqlServerUrls() {
		return Stream.of(
			Arguments.of("jdbc:sqlserver://hostname:1234;databaseName=mydatabase;encrypt=true", "sqlserver", "hostname", "1234", "mydatabase"),
			Arguments.of("jdbc:sqlserver://192.168.1.100:1234;encrypt=true;databaseName=mydatabase", "sqlserver", "192.168.1.100", "1234", "mydatabase"),
			Arguments.of("jdbc:sqlserver://db.example.com;databaseName=mydatabase;trustServerCertificate=true", "sqlserver", "db.example.com", null, "mydatabase")
		);
	}

	static Stream<Arguments> provideOracleUrls() {
		return Stream.of(
			Arguments.of("jdbc:oracle:thin:@localhost:1521:ORCL", "oracle", "localhost", "1521", "ORCL"),
			Arguments.of("jdbc:oracle:thin:@10.100.250.44:1521:ORCL", "oracle", "10.100.250.44", "1521", "ORCL"),
			Arguments.of("jdbc:oracle:thin:@db.example.com:ORCL", "oracle", "db.example.com", null, "ORCL"),
			Arguments.of("jdbc:oracle:thin:@//db.example.com:1521/ORCLPDB1", "oracle", "db.example.com", "1521", "ORCLPDB1")
		);
	}

	static Stream<Arguments> provideClickhouseHttpUrls() {
		return Stream.of(
			Arguments.of("jdbc:clickhouse:https://abc.eu-west-1.aws.clickhouse.cloud:8443/default", "clickhouse", "abc.eu-west-1.aws.clickhouse.cloud", "8443", "default"),
			Arguments.of("jdbc:clickhouse:http://localhost:8123/analytics", "clickhouse", "localhost", "8123", "analytics"),
			Arguments.of("jdbc:clickhouse:http://192.168.1.100/analytics", "clickhouse", "192.168.1.100", null, "analytics")
		);
	}

	static Stream<Arguments> provideDatabricksUrls() {
		return Stream.of(
			Arguments.of("jdbc:databricks://dbc-1234.cloud.databricks.com:443/default;transportMode=http;ssl=1;httpPath=/sql/1.0/warehouses/abc", "databricks", "dbc-1234.cloud.databricks.com", "443", "default"),
			Arguments.of("jdbc:databricks://localhost/default;transportMode=http;ssl=1", "databricks", "localhost", null, "default"),
			Arguments.of("jdbc:databricks://10.100.250.44:443/default", "databricks", "10.100.250.44", "443", "default")
		);
	}

	static Stream<Arguments> provideH2Urls() {
		return Stream.of(
			Arguments.of("jdbc:h2:mem:testdb", "h2", "mem", null, "testdb"),
			Arguments.of("jdbc:h2:file:/path/to/testdb", "h2", "file:/path/to", null, "testdb")
		);
	}

	static Stream<Arguments> provideFallbackUrls() {
		return Stream.of(
			Arguments.of(null, null, null, null, null),
			Arguments.of("http://localhost:8080/mydatabase", null, null, null, null),
			Arguments.of("jdbc:vendor//localhost/mydatabase", null, null, null, null),
			Arguments.of("jdbc:sqlite:/var/data/test.db", null, null, null, null),
			Arguments.of("jdbc:unknown:custom:format", null, null, null, null)
		);
	}

	static void assertURLDecode(String origin, String db, String host, String port, String name) {
		var prt = decode(origin);
		assertEquals(db, prt.getVendor());
		assertEquals(host, prt.getHost());
		assertEquals(port, prt.getPort());
		assertEquals(name, prt.getDatabase());
	}
	
	static String uppercase(String str) {
		return isNull(str) ? null : str.toUpperCase();
	}
}
