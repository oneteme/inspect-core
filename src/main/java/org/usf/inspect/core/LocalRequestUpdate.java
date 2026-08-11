package org.usf.inspect.core;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents an update emitted for a local request, including its actual start time and failure details.
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class LocalRequestUpdate extends AbstractRequestUpdate implements AtomicTrace {

	private Instant start; //real start
	private ExceptionInfo exception; 

	/**
	 * Creates a local request update for the specified request identifier.
	 *
	 * @param id the identifier of the request being updated
	 */
	@JsonCreator
	public LocalRequestUpdate(String id) {
		super(id);
	}
}
