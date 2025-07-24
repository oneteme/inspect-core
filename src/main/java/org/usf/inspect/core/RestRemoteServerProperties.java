package org.usf.inspect.core;

import static java.net.URI.create;
import static java.util.Objects.isNull;
import static org.usf.inspect.core.Assertions.assertPositive;
import static org.usf.inspect.core.Assertions.assertStrictPositive;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@ToString
@JsonIgnoreProperties({"instanceURI", "tracesURI"})
public final class RestRemoteServerProperties implements RemoteServerProperties {
	
	private static final String INSTANCE_DEFAULT_URI = "v3/trace/instance"; //[POST] Sync
	private static final String TRACES_DEFAULT_URI  = "v4/trace/instance/{id}/session"; //[PUT] Async
	
	private URI host = create("http://localhost:9000/");
	private String instanceURI;
	private String tracesURI;
	private int compressMinSize = 0; // size in bytes, 0: no compression
	private int retentionMaxAge = 30; 
	
	@Override
	public void validate() {
		var base = host.resolve("/").toString(); // append '/' if not present
		if(isNull(instanceURI)) {
			instanceURI = base + INSTANCE_DEFAULT_URI;
		}
		if(isNull(tracesURI)) {
			tracesURI = base + TRACES_DEFAULT_URI;
		}
		assertPositive(compressMinSize, "compress-min-size");
		assertStrictPositive(retentionMaxAge, "retention-max-age");
	}
}
