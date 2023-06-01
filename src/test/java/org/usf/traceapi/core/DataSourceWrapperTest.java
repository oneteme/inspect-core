package org.usf.traceapi.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.traceapi.core.DataSourceWrapper.shortURL;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DataSourceWrapperTest {

	@CsvSource({
		"jdbc:teradata://TDHOST/CHARSET=UTF8;DATABASE=my_data;SLOB_RECEIVE_THRESHOLD=15000,TDHOST/my_data",
		"jdbc:teradata://TDHOST/CHARSET=UTF8,TDHOST",
		"jdbc:oracle:thin:@//myoracle.db.server:1521/my_servicename,myoracle.db.server:1521/my_servicename",
		"jdbc:mysql://mysql.db.server:3306/my_database?useSSL=false&serverTimezone=UTC,mysql.db.server:3306/my_database",
		"jdbc:postgresql//10.121.242.56:5432/ome,10.121.242.56:5432/ome"
	})
	@ParameterizedTest
	void testShortURL(String origin, String target) {
		assertEquals(target, shortURL(origin));
	}

}
