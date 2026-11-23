package org.usf.inspect.sql;

import static java.lang.Integer.parseInt;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.sql.JdbcURLDecoder.decode;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 
 * @author u$f
 *
 */
public interface MetadataProvider {

	ConnectionMetadata getMetadata(Connection cnx) throws SQLException;

	static ConnectionMetadata createMetadata(Connection cnx) {
		try {
			var meta = cnx.getMetaData();
			var prt = decode(meta.getURL());
			return new ConnectionMetadata(
					prt.getVendor(),
					prt.getHost(),
					nonNull(prt.getPort()) ? parseInt(prt.getPort()) : -1,
					prt.getDatabase(),
					getSchema(cnx),
					meta.getUserName(),
					meta.getDatabaseProductName(),
					meta.getDatabaseProductVersion(),
					meta.getDriverVersion());
		}
		catch (Exception e) {
			REPORTER.error("DatasourceMetadataProvider.createMetadata: failed to cache metadata for connection {}", cnx, e);
		}
		return null;
	}
	
	static ConnectionMetadata createMetadata(String url) {
		try {
			var prt = decode(url);
			return new ConnectionMetadata(
					prt.getVendor(),
					prt.getHost(),
					nonNull(prt.getPort()) ? parseInt(prt.getPort()) : -1,
					prt.getDatabase(), null, null, null, null, null);
		}
		catch (Exception e) {
			REPORTER.error("DatasourceMetadataProvider.createMetadata: failed to cache metadata for url {}", url, e);
		}
		return null;
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