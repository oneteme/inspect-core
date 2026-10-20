package org.usf.inspect.core;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableList;
import static org.usf.inspect.InspectCollectorProperties.initializeConfiguration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.springframework.core.env.StandardEnvironment;
import org.usf.inspect.EventTrace;
import org.usf.inspect.util.DisabledConfiguration;

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
	
	private static final InspectConfiguration enabledConfig = new InspectConfiguration(initializeConfiguration(true), new StandardEnvironment());
	private static final InspectConfiguration disabledConfig = new InspectConfiguration(new StandardEnvironment());

	@Override
	public void emitTrace(EventTrace trace) {
		traces.add(trace);
	}
	
	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		processWithTestHub(invocation, extensionContext);
	}

	@Override
	public void interceptTestTemplateMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		processWithTestHub(invocation, extensionContext);
	}
	
	public static List<EventTrace> getTraces() {
		return unmodifiableList(INSTANCE.traces);
	}
	
	static void processWithTestHub(Invocation<Void> invocation, ExtensionContext context) throws Throwable {
		
		boolean isDisabled = context.getTestMethod()
	            .map(method -> method.isAnnotationPresent(DisabledConfiguration.class))
	            .orElse(false);
		var prv = TraceHub.hub();
		var prvConfig = InspectConfiguration.INSTANCE;
		try {
			InspectConfiguration.INSTANCE = isDisabled ? disabledConfig : enabledConfig;
			TraceHub.Holder.INSTANCE = INSTANCE;
			INSTANCE.traces.clear();
			invocation.proceed();
		} finally {
			TraceHub.Holder.INSTANCE = prv;
			InspectConfiguration.INSTANCE = prvConfig;
		}
	}
}