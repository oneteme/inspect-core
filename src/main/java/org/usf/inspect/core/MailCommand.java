package org.usf.inspect.core;

import static org.usf.inspect.core.CommandType.EMIT;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumerates the supported mail-related commands and their corresponding command types.
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
