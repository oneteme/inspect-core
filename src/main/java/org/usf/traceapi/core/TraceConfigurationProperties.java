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
	
	private String url = "";
	private int delay = 5;
	private TimeUnit unit = SECONDS;

	public void setUnit(String unit){
		this.unit = TimeUnit.valueOf(unit.toUpperCase());
	}

}
