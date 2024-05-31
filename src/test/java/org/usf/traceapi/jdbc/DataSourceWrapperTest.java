package org.usf.traceapi.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.traceapi.jdbc.DataSourceWrapper.decodeURL;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DataSourceWrapperTest {

	@CsvSource(nullValues = {""}, value={
		"jdbc:teradata://TDHOST/CHARSET=UTF8;DATABASE=my_data;SLOB_RECEIVE_THRESHOLD=15000,TDHOST,,my_data",
		"jdbc:teradata://TDHOST:666/database=my_data;CHARSET=UTF8;SLOB_RECEIVE_THRESHOLD=15000,TDHOST,666,my_data",
		"jdbc:teradata://TDHOST/CHARSET=UTF8,TDHOST,,",
		"jdbc:oracle:thin:@//myoracle.db.server:1521/my_servicename,myoracle.db.server,1521,my_servicename",
		"jdbc:mysql://mysql.db.server:3306/my_database?useSSL=false&serverTimezone=UTC,mysql.db.server,3306,my_database",
		"jdbc:postgresql//10.123.321.44:4455/db,10.123.321.44,4455,db"})
	@ParameterizedTest
	void testShortURL(String origin, String host, String port, String schema) {
		var arr = decodeURL(origin);
		assertEquals(host, arr[0]);
		assertEquals(port, arr[1]);
		assertEquals(schema, arr[2]);
	}

}
