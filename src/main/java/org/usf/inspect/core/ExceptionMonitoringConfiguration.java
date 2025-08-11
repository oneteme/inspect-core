package org.usf.inspect.core;

import static org.usf.inspect.core.Assertions.assertGreaterOrEquals;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public final class ExceptionMonitoringConfiguration {
	
	private int maxStackTraceRows = 5; // max rows in stack trace per exception, -1 means no limit
	private int maxCauseDepth = -1; // max depth of cause chain, -1 means no limit
	//private String[] classNamePatterns = null 
	
	void validate() {
		assertGreaterOrEquals(maxStackTraceRows, -1, "stack-trace-rows");
		assertGreaterOrEquals(maxCauseDepth, -1, "max-cause-depth");
	}
}
