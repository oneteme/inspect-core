package org.usf.inspect.core;

import static org.usf.inspect.core.TraceDispatcherHub.initializeTraceHub;

import java.util.ArrayList;
import java.util.List;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class TestTraceHub implements TraceHub {
	
	private static final TestTraceHub INSTANCE = (TestTraceHub) initializeTraceHub(new TestTraceHub());
	
	private final List<EventTrace> traces = new ArrayList<>();

	@Override
	public InspectCollectorConfiguration getConfiguration() {
		var config = new InspectCollectorConfiguration();
		config.setEnabled(true);
		return config;
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