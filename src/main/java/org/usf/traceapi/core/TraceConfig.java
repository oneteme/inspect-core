package org.usf.traceapi.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "api.tracing")
public final class TraceConfig {
	
	private String application = "";
	private String url = "";
	private int delay = 5;
	private String unit = "SECONDS";

}
