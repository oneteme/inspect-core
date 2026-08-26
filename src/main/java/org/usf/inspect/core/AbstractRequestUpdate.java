package org.usf.inspect.core;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
public class AbstractRequestUpdate implements TraceUpdate, HasStage {

	@JsonIgnore 
	private final AtomicInteger stageCounter = new AtomicInteger();
	
	private final UUID id;
	private String command; //READ, EMIT, EDIT, ..
	private Instant end;
	//v1.2
	private int status = -1; //-1 unknown
}
