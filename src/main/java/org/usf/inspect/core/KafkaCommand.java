package org.usf.inspect.core;

import static org.usf.inspect.core.CommandType.EMIT;
import static org.usf.inspect.core.CommandType.READ;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public enum KafkaCommand {

	SEND(EMIT), POLL(READ), OFFSET_COMMIT(READ);

	private final CommandType type;
}
