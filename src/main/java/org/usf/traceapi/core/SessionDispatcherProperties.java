package org.usf.traceapi.core;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "trace")
public class SessionDispatcherProperties {
	
    private int delay = 5;
	private TimeUnit unit = SECONDS;
	private int bufferSize = 100; // {n} sessions
	private int bufferMaxSize = 5_000; // {n} sessions, -1 : unlimited

	public void setDelay(int delay) {
		this.delay = requiePositiveValue(delay, "delay");
	}
	
	public void setUnit(String unit){
		this.unit = TimeUnit.valueOf(unit.toUpperCase());
	}
	
	public void setBufferSize(int bufferSize) { //initial size
		this.bufferSize = requiePositiveValue(bufferSize, "bufferSize");
	}

	public void setBufferMaxSize(int bufferMaxSize) {
		this.bufferMaxSize = bufferMaxSize;
	}

	private static int requiePositiveValue(int v, String name) {
		if(v > 0) {
			return v;
		}
		throw new IllegalArgumentException("trace." + name + "=" +  v + " <= 0");
	}
}
