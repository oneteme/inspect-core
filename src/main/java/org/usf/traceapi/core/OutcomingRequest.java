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

	private final String id;
	private String url;
	private String method;
	private long start;
	private long end;
	private Integer status; //nullable
}