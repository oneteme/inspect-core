package org.usf.inspect.test;

import static java.time.Clock.systemUTC;
import static org.usf.inspect.Helper.formatLocation;
import static org.usf.inspect.MainSessionType.TEST;
import static org.usf.inspect.core.Constants.JUNIT5_ENABLED;
import static org.usf.inspect.core.ExecutionTracer.mainSessionTracer;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;
import static org.usf.inspect.core.InspectExecutor.call;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * 
 * @author u$f
 *
 */
public final class Junit5TestMonitor implements InvocationInterceptor {
		
	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		
		processInvocation(invocation, extensionContext, invocationContext, "beforeAll_");
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		processInvocation(invocation, extensionContext, invocationContext, "before_");
	}
	
	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		processInvocation(invocation, extensionContext, invocationContext, "aferAll_");
	}
	
	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		processInvocation(invocation, extensionContext, invocationContext, "after_");
	}
	
	@Override
	public void interceptTestTemplateMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		processInvocation(invocation, extensionContext, invocationContext, "");
	}
	
	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		
		processInvocation(invocation, extensionContext, invocationContext, "");
	}
	
//	@Override
//	public void interceptDynamicTest(Invocation<Void> invocation, DynamicTestInvocationContext invocationContext,
//			ExtensionContext extensionContext) throws Throwable {
//		
//		processInvocation(invocation, extensionContext, "");
//	}
	
	
	static <T> T processInvocation(Invocation<T> invocation, ExtensionContext extensionContext, ReflectiveInvocationContext<Method> invocationContext, String prefix) throws Throwable {
		if(getConfiguration().isEnabled(JUNIT5_ENABLED, true)) {
			return call(invocation::proceed, mainSessionTracer(systemUTC().instant(), TEST, sgn-> {
				//TODO before/after method name
				sgn.setName(prefix + extensionContext.getDisplayName());
				sgn.setLocation(formatLocation(invocationContext.getTargetClass().getName(), invocationContext.getExecutable().getName()));
				//set test user
			}));
		}
		return invocation.proceed();
	}
}
