package org.usf.traceapi.core;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@ConfigurationProperties(prefix = "api.tracing")
public final class TraceConfigurationProperties {
	
	private String url = "";
	private int delay = 5;
	private TimeUnit unit = SECONDS;
	private int maxCachedSession = 100_000;

	public void setUrl(String url) {
		this.url = url;
	}

	public void setDelay(int delay) {
		this.delay = requiePositiveValue(delay);
	}
	
	public void setUnit(String unit){
		this.unit = TimeUnit.valueOf(unit.toUpperCase());
	}

	public void setMaxCachedSession(int maxCachedSession) {
		this.maxCachedSession = requiePositiveValue(maxCachedSession);
	}

	private static int requiePositiveValue(int v) {
		if(v <= 0) {
			throw new IllegalArgumentException(v + " <= 0");
		}
		return v;
	}
	
}
