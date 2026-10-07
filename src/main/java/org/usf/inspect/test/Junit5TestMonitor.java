package org.usf.inspect.test;

import static java.time.Clock.systemUTC;
import static org.usf.inspect.core.ExecutionTracer.forMainSession;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.MainSessionType.TEST;
import static org.usf.inspect.core.TraceHub.hub;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * 
 * @author u$f
 *
 */
@InspectTest
public final class Junit5TestMonitor implements InvocationInterceptor {

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		
		processInvocation(invocation, extensionContext, "Junit5TestMonitor.interceptBeforeAllMethod");
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		
		processInvocation(invocation, extensionContext, "Junit5TestMonitor.interceptBeforeEachMethod");
	}
	
	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		processInvocation(invocation, extensionContext, "Junit5TestMonitor.interceptAfterAllMethod");
	}
	
	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		processInvocation(invocation, extensionContext, "Junit5TestMonitor.interceptAfterEachMethod");
	}
	
	
	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		
		processInvocation(invocation, extensionContext, "Junit5TestMonitor.interceptTestMethod");
	}
	
	@Override
	public void interceptDynamicTest(Invocation<Void> invocation, DynamicTestInvocationContext invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		
		processInvocation(invocation, extensionContext, "Junit5TestMonitor.interceptDynamicTest");
	}
	
	static void processInvocation(Invocation<Void> invocation, ExtensionContext extensionContext, String action) throws Throwable {
		if(hub().isEnabled()) {
			call(invocation::proceed, forMainSession(systemUTC().instant(), TEST, sgn-> {
				sgn.setName(extensionContext.getDisplayName());
				//TODO before/after method name
				sgn.setLocation(formatLocation(extensionContext.getRequiredTestClass().getName(), extensionContext.getRequiredTestMethod().getName()));
				//set test user
			}));
		}
		else {
			invocation.proceed();
		}
	}
}
