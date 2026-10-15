package org.usf.inspect;

import static org.usf.inspect.CommandType.EMIT;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public enum MailCommand {

	SEND(EMIT);

	private final CommandType type;
}
