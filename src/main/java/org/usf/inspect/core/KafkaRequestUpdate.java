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
public final class KafkaRequestUpdate extends AbstractRequestUpdate {

	private int status; // 0=success, 1=failed, -1=unknown
	private long recordSize; // in bytes, -1 if unknown
	private String recordKey;
	private int recordCount; // for batch operations

	@JsonCreator
	public KafkaRequestUpdate(String id) {
		super(id);
	}

	public KafkaRequestStage createStage(KafkaAction action, Instant start, Instant end, Throwable thrw, KafkaCommand cmd) {
		return createStage(action, start, end, cmd, thrw, KafkaRequestStage::new);
	}
}
