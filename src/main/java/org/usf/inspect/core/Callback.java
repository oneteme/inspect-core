package org.usf.inspect.core;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorReporter.reportMessage;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface Callback extends CompletableTrace {
	
	Instant getEnd();

	public static boolean assertStillOpened(Callback callback) {
		if(nonNull(callback)) {
			if(isNull(callback.getEnd())) {
				return true;
			}
			reportMessage(true, "assertStillOpened", format("'%s' is already closed", callback.getClass().getSimpleName()));
		}
		reportMessage(true, "assertStillOpened", "callback is null");
		return false;
	}
}
