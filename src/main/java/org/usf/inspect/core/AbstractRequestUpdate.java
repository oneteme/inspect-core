package org.usf.inspect.core;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Base update that stores mutable state for a traced request.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class AbstractRequestUpdate implements TraceUpdate, HasStage {

	@JsonIgnore 
	private final AtomicInteger stageCounter = new AtomicInteger();
	
	private final String id;
	private String command; //READ, EMIT, EDIT etc. (Enum from CommandType.java)
	private Instant end;
}
