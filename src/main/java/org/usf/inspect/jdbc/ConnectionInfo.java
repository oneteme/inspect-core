package org.usf.inspect.jdbc;

import static java.lang.Integer.parseInt;
import static java.util.Objects.nonNull;
import static org.usf.inspect.jdbc.JdbcURLDecoder.decode;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * 
 * @author u$f
 *
 */
public final record ConnectionInfo(String scheme, String host, int port, String name,
		String schema, String user, String productName, String productVersion, String driverVersion) {

	public static ConnectionInfo fromMetadata(DatabaseMetaData meta) throws SQLException {
		var arr = decode(meta.getURL());
		return new ConnectionInfo(arr[0], arr[1], nonNull(arr[2]) ? parseInt(arr[2]) : -1, arr[3],
				getSchema(meta.getConnection()), meta.getUserName(),
				meta.getDatabaseProductName(), meta.getDatabaseProductVersion(), meta.getDriverVersion());
	}

	static String getSchema(Connection cnx) {
		try { 
			return cnx.getSchema(); //PG: select current_schema()
		}
		catch (Throwable e) { //
			// do not throw exception
			// Teradata does not define or inherit an implementation of the resolved method getSchema
		}
		return null;
	}
}