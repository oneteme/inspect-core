package org.usf.inspect.core;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableList;
import static org.usf.inspect.core.InspectCollectorConfiguration.initializeConfiguration;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
@Getter
public class TestTraceHub implements TraceHub {

	private static final TestTraceHub INSTANCE = new TestTraceHub();
	
	private final List<EventTrace> traces = synchronizedList(new ArrayList<>());
	private final InspectCollectorConfiguration configuration = initializeConfiguration(true);
	
	static {
		TraceHub.Holder.INSTANCE = INSTANCE;
	}

	@Override
	public boolean emitTrace(EventTrace trace) {
		return traces.add(trace);
	}
	
	public static List<EventTrace> getTraces() {
		return unmodifiableList(INSTANCE.traces);
	}
	
	public static void clearTraces() {
		INSTANCE.traces.clear();
	}
}