package org.usf.inspect.core;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableList;
import static org.usf.inspect.core.InspectCollectorConfiguration.initializeConfiguration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
@Getter
public class TestTraceHub implements TraceHub, InvocationInterceptor {

	private static final TestTraceHub INSTANCE = new TestTraceHub();
	
	private final List<EventTrace> traces = synchronizedList(new ArrayList<>());
	private final InspectCollectorConfiguration configuration = initializeConfiguration(true);

	@Override
	public boolean emitTrace(EventTrace trace) {
		return traces.add(trace);
	}
	
	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		processWithTestHub(invocation);
	}
	
	@Override
	public void interceptDynamicTest(Invocation<Void> invocation, DynamicTestInvocationContext invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		processWithTestHub(invocation);
	}
	
	public static List<EventTrace> getTraces() {
		return unmodifiableList(INSTANCE.traces);
	}
	
	public static void clearTraces() {
		INSTANCE.traces.clear();
	}
	
	static void processWithTestHub(Invocation<Void> invocation) throws Throwable {
		var prv = TraceHub.hub();
		try {
			TraceHub.Holder.INSTANCE = INSTANCE;
			invocation.proceed();
		} finally {
			TraceHub.Holder.INSTANCE = prv;
		}
	}
}