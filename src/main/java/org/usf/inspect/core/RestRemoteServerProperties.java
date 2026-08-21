package org.usf.inspect.core;

import static java.net.URI.create;
import static java.time.Duration.ofDays;
import static java.util.Objects.isNull;
import static org.usf.inspect.core.Assertions.assertAbsolute;
import static org.usf.inspect.core.Assertions.assertBetween;
import static org.usf.inspect.core.Assertions.assertIdentifier;
import static org.usf.inspect.core.Assertions.assertPositive;

import java.net.URI;

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
public final class RestRemoteServerProperties implements RemoteServerProperties {
	
	private URI host = create("http://localhost:9000/");
	private String instanceURI = "/v5/trace/instance"; //[POST] Sync
	private String tracesURI = "/v5/trace/instance/{id}/session"; //[PUT] Async
	private int compressMinSize = 0; // size in bytes, 0: no compression
	//v1.2
	private Retention retentionMaxAge = new Retention();
	private String namespace;
	private String token; //optional
	
	@Override
	public void validate() {
		assertAbsolute(host, "host");
		var base = host.resolve("/").toString(); // append '/' if not present
		instanceURI = base + instanceURI;
		tracesURI = base + tracesURI;
		assertPositive(compressMinSize, "compress-min-size");
		assertBetween(retentionMaxAge.getAudit(), ofDays(1), ofDays(365), "retention.audit");
		assertBetween(retentionMaxAge.getDiagnostic(), ofDays(1), ofDays(365), "retention.diagnostic");
		if(isNull(namespace)) {
			namespace = "";
		}
		else {
			assertIdentifier(namespace, "namespace");
		}
		if(isNull(token)) {
			token = "";
		}
	}
}
