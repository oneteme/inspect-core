package org.usf.inspect.jdbc;

import static java.lang.Integer.parseInt;
import static java.util.Objects.nonNull;
import static org.usf.inspect.jdbc.JdbcURLDecoder.decode;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
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
	
	public void update(DatabaseMetaData meta) throws SQLException {
		var prt = decode(meta.getURL());
		this.scheme = prt.getHost();
		this.host = prt.getHost();
		this.port = nonNull(prt.getPort()) ? parseInt(prt.getPort()) : -1;
		this.name = prt.getDatabase();
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
