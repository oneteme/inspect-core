package org.usf.inspect;

import static org.usf.inspect.TraceType.SMTP_REQ;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
public final class MailRequestUpdate extends AbstractRequestUpdate {

	@Deprecated(forRemoval = false, since = "1.2")
	private boolean failed;

	@JsonCreator
	public MailRequestUpdate(UUID id) {
		super(id);
	}
	
	@Override
	public byte traceType() {
		return SMTP_REQ.getValue();
	}
}
