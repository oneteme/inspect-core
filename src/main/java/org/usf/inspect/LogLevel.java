package org.usf.inspect;

import static org.usf.inspect.Events.*;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public enum LogLevel {
	
	INFO(INFO_ID), WARN(WARN_ID), ERROR(ERROR_ID), ALERT(ALERT_ID), 
	@Deprecated(forRemoval = true, since = "v1.2") REPORT(-1);

	private final int code;

}