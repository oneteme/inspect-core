package org.usf.traceapi.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@ConfigurationProperties(prefix = "api.tracing")
public final class TraceConfigurationProperties extends SessionDispatcherProperties {
	
	private String url = "";

	public void setUrl(String url) {
		this.url = url;
	}
}
