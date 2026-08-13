package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class KafkaRequestSignal extends AbstractRequestSignal {

	private String topic;
	private int partition; // -1 if unknown
	private String brokers;
	private String cluster;

	public KafkaRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	public KafkaRequestUpdate createCallback() {
		return new KafkaRequestUpdate(getId());
	}
}
