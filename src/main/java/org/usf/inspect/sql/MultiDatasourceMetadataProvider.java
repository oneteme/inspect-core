package org.usf.inspect.sql;

import static java.util.Objects.nonNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author u$f
 *
 */
public final class MultiDatasourceMetadataProvider implements MetadataProvider {
	
	private final Map<String, ConnectionMetadata> map = new ConcurrentHashMap<>();

	@Override
	public ConnectionMetadata getMetadata(Connection cnx) throws SQLException {
		if(nonNull(cnx)) {
			var meta = cnx.getMetaData();
			return map.computeIfAbsent(meta.getURL(), k-> MetadataProvider.createMetadata(cnx));
		}
		return null;
	}
}
