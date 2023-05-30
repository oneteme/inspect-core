package org.usf.traceapi.core;

import static java.time.Duration.between;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class OutcomingRequest {

	private final String id;
	private String url;
	private String method;
	private Integer status; //nullable
	private Instant start;
	private Instant end;
	
	@Override
	public String toString() {
		return "REQUEST {" + between(start, end).toMillis() + "ms}";
	}
}