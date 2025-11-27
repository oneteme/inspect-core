package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorReporter.stackReporter;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface Callback extends Compleatable {
	
	Instant getEnd();

	public static boolean assertStillOpened(Callback callback) {
		if(nonNull(callback)) {
			if(nonNull(callback.getEnd())) {
				return true;
			}
			stackReporter().message("callack was closed").emit();
		}
		stackReporter().message("callback is null").thread().emit();
		return false;
	}
}
