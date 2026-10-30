package org.usf.inspect;

import static org.usf.inspect.Assertions.assertGreaterOrEquals;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public final class ExceptionMonitoringProperties {
	
	//enabled
	private int maxStackTraceRows = 5; // max rows in stack trace per exception, -1 means no limit
	private int maxCauseDepth = -1; // max depth of cause chain, -1 means no limit
	//private String[] classNamePatterns = null 
	//v1.2
	private boolean enabled = true;
	
	void validate() {
		assertGreaterOrEquals(maxStackTraceRows, -1, "stack-trace-rows");
		assertGreaterOrEquals(maxCauseDepth, -1, "max-cause-depth");
	}
}
