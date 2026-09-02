package org.usf.inspect.core;

import java.util.ArrayList;
import java.util.List;

import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.InspectCollectorConfiguration;
import org.usf.inspect.core.TraceHub;

import lombok.Getter;

public class TestTraceHub implements TraceHub {

	@Getter
	private final List<EventTrace> traces = new ArrayList<>();

	@Override
	public InspectCollectorConfiguration getConfiguration() {
		return null; //TODO check this
	}

	@Override
	public boolean emitTrace(EventTrace trace) {
		return traces.add(trace);
	}
}