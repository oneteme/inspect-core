package org.usf.traceapi.core;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class SubRequest {

	private final String url;
	private final String method;
	private final long start;
	private long end;
	private Integer status;
}