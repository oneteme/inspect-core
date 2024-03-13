package org.usf.traceapi.core;

import lombok.Getter;
import lombok.Setter;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "trace")
public class SessionDispatcherProperties {
	
    private int delay;
	private TimeUnit unit = SECONDS;
	private int bufferSize = 100; // session count
	private int bufferMaxSize = 5_000; // session count, -1 : unlimited

	public void setDelay(int delay) {
		this.delay = requiePositiveValue(delay, "delay");
	}
	
	public void setUnit(String unit){
		this.unit = TimeUnit.valueOf(unit.toUpperCase());
	}
	
	public void setBufferSize(int bufferSize) {
		this.bufferSize = requiePositiveValue(bufferSize, "bufferSize");
	}

	public void setBufferMaxSize(int bufferMaxSize) {
		this.bufferMaxSize = bufferMaxSize;
	}

	private static int requiePositiveValue(int v, String name) {
		if(v > 0) {
			return v;
		}
		throw new IllegalArgumentException(name + "=" +  v + " <= 0");
	}
}
