package org.usf.inspect.core;

import static org.usf.inspect.core.TraceType.LCL_REQ;

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
public final class LocalRequestUpdate extends AbstractRequestUpdate {

	private Instant start; //real start
	
	@Deprecated(forRemoval = true, since = "v1.2")
	private ExceptionTrace exception; 

	@JsonCreator
	public LocalRequestUpdate(UUID id) {
		super(id);
	}
	
	@Override
	public byte traceType() {
		return LCL_REQ.getValue();
	}
}
