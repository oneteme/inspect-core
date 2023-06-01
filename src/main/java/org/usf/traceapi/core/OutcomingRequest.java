package org.usf.traceapi.core;

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
	private Integer port;
	private String path;
	private String query;
	private String method;
	private Integer status; //nullable
	private long size;
 	private Instant start;
	private Instant end;
	
	@Override
	public String toString() {
		return "REQUEST {" + between(start, end).toMillis() + "ms}";
	}
}