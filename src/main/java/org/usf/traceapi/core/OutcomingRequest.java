package org.usf.traceapi.core;

import static java.lang.String.format;
import static java.time.Duration.between;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@RequiredArgsConstructor
public class OutcomingRequest {

	private final String id;
	private String protocol;
	private String host;
	private int port;
	private String path;
	private String query;
	private String method;
	private Integer status; //nullable
	private long size;
 	private Instant start;
	private Instant end;
	private String thread;
	
	@Override
	public String toString() {
		return format("%-20s", thread) + ": REQUEST {" +  format("%5s", between(start, end).toMillis()) + "ms}";
	}
}