package org.usf.traceapi.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@RequiredArgsConstructor
public class DatabaseAction {

	private final Action type;
	private final long start;
	private final long end;
	private final boolean failed;
	//private final long count;
	
}