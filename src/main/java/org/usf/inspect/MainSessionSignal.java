package org.usf.inspect;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f 
 *
 */
@Getter
@Setter
public final class MainSessionSignal extends AbstractSessionSignal {

	private String type; //v1.1 => v1.2 type conversion
	//v1.2
	private UUID parentId;
	
	public MainSessionSignal(UUID id, Instant start, String threadName, String type) {
		super(id, start, threadName);
		this.type = type;
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withInstant(getStart())
				.withThread(getThreadName())
				.withAction(getType())
				.withUser(getUser())
				.withArgsAsTopic(getLocation(), null)
				.format();
	}
}
