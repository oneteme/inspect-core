package org.usf.inspect.jdbc;

import static java.lang.Integer.parseInt;
import static java.util.Objects.nonNull;
import static org.usf.inspect.jdbc.JdbcURLDecoder.decode;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * 
 * @author u$f
 *
 */
public final record ConnectionInfo(String scheme, String host, int port, String name) {

	public static ConnectionInfo fromMetadata(DatabaseMetaData meta) throws SQLException {
		var args = decode(meta.getURL());
		return new ConnectionInfo(args[0], args[1], nonNull(args[2]) ? parseInt(args[2]) : -1, args[3]);
	}
}