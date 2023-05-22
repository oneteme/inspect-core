package org.usf.traceapi.core;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@ToString
@RequiredArgsConstructor
public class OutcomingQuery {
	
	//action SELECT | UPDATE
	private final long start;
	private final long end;
	private final boolean failed;

}
