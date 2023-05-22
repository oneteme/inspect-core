package org.usf.traceapi.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class OutcomingRequest {

	private final String url;
	private final String method;
	private final long start;
	private long end;
	private Integer status; //nullable
}