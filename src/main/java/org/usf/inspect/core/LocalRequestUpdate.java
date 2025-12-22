package org.usf.inspect.core;

import java.time.Instant;

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
	private ExceptionInfo exception; 

	@JsonCreator
	public LocalRequestUpdate(String id) {
		super(id);
	}
}
