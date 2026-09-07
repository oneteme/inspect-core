package org.usf.inspect.core;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class LocalRequestUpdate extends AbstractRequestUpdate implements AtomicTrace {

	private Instant start; //real start
	
	@Deprecated(forRemoval = true, since = "v1.2")
	private ExceptionTrace exception; 

	@JsonCreator
	public LocalRequestUpdate(UUID id) {
		super(id);
	}
}
