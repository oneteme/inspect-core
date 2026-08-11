package org.usf.inspect.jdbc;

import static java.lang.Integer.parseInt;
import static java.util.Objects.nonNull;
import static org.usf.inspect.jdbc.JdbcURLDecoder.decode;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import lombok.Getter;

/**
 * Caches database connection metadata extracted from JDBC metadata.
 *
 * @author u$f
 */
@Getter
final class ConnectionMetadataCache {

	private String scheme;
	private String host;
	private int port;
	private String name;
	private String schema; 
	private String user;
	private String productName; 
	private String productVersion; 
	private String driverVersion;
	private boolean present;
	
	/**
	 * Updates the cached metadata from the given JDBC metadata instance.
	 *
	 * @param meta the JDBC metadata to read
	 * @throws SQLException if the metadata cannot be read
	 */
	public void update(DatabaseMetaData meta) throws SQLException {
		var arr = decode(meta.getURL());
		this.scheme = arr[0];
		this.host = arr[1]; 
		this.port = nonNull(arr[2]) ? parseInt(arr[2]) : -1;
		this.name = arr[3];
		this.schema = getSchema(meta.getConnection());
		this.user = meta.getUserName();
		this.productName = meta.getDatabaseProductName();
		this.productVersion = meta.getDatabaseProductVersion();
		this.driverVersion = meta.getDriverVersion();
		this.present = true;
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
