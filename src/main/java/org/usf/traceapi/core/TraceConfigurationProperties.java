package org.usf.traceapi.core;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "api.tracing")
public final class TraceConfigurationProperties {
	
	private String host = "";
	private int delay = 5;
	private TimeUnit unit = SECONDS;
	private String basePackage = "";

	public void setHost(String host) {
		this.host = normalizeHost(host);
	}
		
	public void setUnit(String unit){
		this.unit = TimeUnit.valueOf(unit.toUpperCase());
	}

	private static String normalizeHost(String host) {
		return host.endsWith("/") ? host.substring(0, host.length()-1) : host;
	}
	
	public static class Exclude {

		private String[] paths;
		private String[] methods;
	}
}
