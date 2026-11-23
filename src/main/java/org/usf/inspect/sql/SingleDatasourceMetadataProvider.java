package org.usf.inspect.sql;

import static java.util.Objects.nonNull;

import java.sql.Connection;

import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor
public final class SingleDatasourceMetadataProvider implements MetadataProvider {

	private ConnectionMetadata metadata;
	private volatile boolean complete;
	
	public SingleDatasourceMetadataProvider(String url) {
		if(nonNull(url)) {
			this.metadata = MetadataProvider.createMetadata(url);
		}
	}
	
	@Override
	public ConnectionMetadata getMetadata(Connection cnx) {
		if(!complete && nonNull(cnx)) {
			synchronized (this) {
				var meta = MetadataProvider.createMetadata(cnx);
				if(nonNull(meta)) {
					metadata = meta;
					complete = true;
				}
			}
		}
		return metadata;
	}
}
