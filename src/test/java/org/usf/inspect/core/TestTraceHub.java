package org.usf.inspect.core;

import static org.usf.inspect.core.TraceDispatcherHub.initializeTraceHub;

import java.util.ArrayList;
import java.util.List;

import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class TestTraceHub implements TraceHub {
	
	private static final TestTraceHub INSTANCE;
	
	private final List<EventTrace> traces = new ArrayList<>();
	
	static {
		var config = new InspectCollectorConfiguration();
		config.setEnabled(true);
		INSTANCE = (TestTraceHub) initializeTraceHub(new TestTraceHub());
	}

	@Override
	public InspectCollectorConfiguration getConfiguration() {
		return INSTANCE.getConfiguration();
	}

	@Override
	public boolean emitTrace(EventTrace trace) {
		return traces.add(trace);
	}
	
	public static List<EventTrace> getTraces() {
		return INSTANCE.traces;
	}
	
	public static void clearTraces() {
		INSTANCE.traces.clear();
	}
}