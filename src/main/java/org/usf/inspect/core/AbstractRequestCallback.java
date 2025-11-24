package org.usf.inspect.core;

import static org.usf.inspect.core.ErrorReporter.reporter;

import java.time.Instant;
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
public class AbstractRequestCallback implements Callback, HasStage {

	@JsonIgnore private final AtomicInteger stageCounter = new AtomicInteger();
	
	private final String id;
	private String command;
	private Instant end;
	
	public static void reportNoActiveRequest(String action, RequestMask mask) {
		reporter().action(action).message("no active request["+mask+"]").thread().emit();
	}
}
