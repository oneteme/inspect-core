package org.usf.inspect;

import static org.usf.inspect.TraceType.MAIN_SES;

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
public final class MainSessionUpdate extends AbstractSessionUpdate {

	@JsonCreator
	public MainSessionUpdate(UUID id) {
		super(id);
	}
	
	@Override
	public byte traceType() {
		return MAIN_SES.getValue();
	}
}
