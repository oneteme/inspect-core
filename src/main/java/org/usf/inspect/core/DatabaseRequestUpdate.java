package org.usf.inspect.core;

import static org.usf.inspect.core.TraceType.JDBC_REQ;

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
public final class DatabaseRequestUpdate extends AbstractRequestUpdate {

	@Deprecated(forRemoval = false, since = "1.2")
	private boolean failed;

	@JsonCreator
	public DatabaseRequestUpdate(UUID id) {
		super(id);
	}
	
	@Override
	public byte traceType() {
		return JDBC_REQ.getValue();
	}
}
