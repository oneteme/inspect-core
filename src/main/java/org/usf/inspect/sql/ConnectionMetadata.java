package org.usf.inspect.sql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
final class ConnectionMetadata {
	
	private final String scheme;
	private final String host;
	private final int port;
	private final String name;
	private final String schema;
	private final String user;
	private final String productName; 
	private final String productVersion; 
	private final String driverVersion;

}
