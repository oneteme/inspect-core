package org.usf.inspect.core;

import static java.net.URI.create;
import static java.time.Duration.ofDays;
import static org.usf.inspect.core.Assertions.assertAbsolute;
import static org.usf.inspect.core.Assertions.assertBetween;
import static org.usf.inspect.core.Assertions.assertPositive;

import java.net.URI;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonSetter;
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
	private String instanceURI = "v4/trace/instance"; //[POST] Sync
	private String tracesURI = "v4/trace/instance/{id}/session"; //[PUT] Async
	private int compressMinSize = 0; // size in bytes, 0: no compression
	//v1.1
	//private Duration retentionMaxAge = ofDays(30);

	private Retention retention = new Retention();

	@JsonSetter("retentionMaxAge")
	public void setRetentionMaxAge(Object raw) {
		Duration d = (raw instanceof Number n)
				? Duration.ofDays(n.longValue())
				: (raw instanceof String s ? Duration.parse(s)
				: (raw instanceof Duration duration ? duration : Duration.ofDays(30)));

		this.retention.setDiagnostic(d);
		this.retention.setAudit(d);
	}
	
	@Override
	public void validate() {
		assertAbsolute(host, "host");
		var base = host.resolve("/").toString(); // append '/' if not present
		instanceURI = base + instanceURI;
		tracesURI = base + tracesURI;
		assertPositive(compressMinSize, "compress-min-size");
		if (retention == null) {
			retention = new Retention(); // defaults
		}
		assertBetween(retention.getDiagnostic(), ofDays(1), ofDays(365), "retention.diagnostic");
		assertBetween(retention.getAudit(), ofDays(1), ofDays(365), "retention.audit");

	}
}
