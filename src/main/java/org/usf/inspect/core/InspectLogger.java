package org.usf.inspect.core;

import static java.time.Clock.systemUTC;
import static org.usf.inspect.Helper.threadName;
import static org.usf.inspect.LogLevel.ALERT;
import static org.usf.inspect.LogLevel.ERROR;
import static org.usf.inspect.LogLevel.INFO;
import static org.usf.inspect.LogLevel.WARN;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;
import static org.usf.inspect.core.SessionContextManager.createLog;
import static org.usf.inspect.core.TraceHub.hub;

import org.usf.inspect.LogLevel;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InspectLogger {

	public static void emitInfo(String msg) {
		emitLog(INFO, msg);
	}

	public static void emitWarn(String msg) {
		emitLog(WARN, msg);
	}

	public static void emitError(String msg) {
		emitLog(ERROR, msg);
	}
	
	static void emitAlert(String msg) {
		emitLog(ALERT, msg); //dispatch immediately 
	}
	
	public static void emitLog(@NonNull LogLevel level, String msg) {
		if(getConfiguration().isEnabled()) {
			hub().emitTrace(createLog(systemUTC().instant(), threadName(), level.name(), msg, null));
		}
	}
}
