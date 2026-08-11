package org.usf.inspect.test;

import static java.time.Clock.systemUTC;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.usf.inspect.core.Monitor.assertMonitorNonNull;
import static org.usf.inspect.core.Monitor.traceAroundMethod;
import static org.usf.inspect.core.SessionContextManager.createTestSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;

import java.util.Optional;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestWatcher;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

/**
 * Monitors JUnit 5 test execution and records test lifecycle events for Inspect.
 */
public final class Junit5TestMonitor implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, TestWatcher, AfterAllCallback {

	private static final Namespace NAMESPACE = create(Junit5TestMonitor.class.getName());
	private static final String SESSION_KEY = "inspect-junit-monitor";
	
	/**
	 * Initializes a fallback test session before the test class lifecycle starts.
	 *
	 * @param context the current extension context.
	 * @throws Exception if the setup fails.
	 */
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		setActiveContext(createTestSession(systemUTC().instant()).createCallback()); //fake session, avoid no active session
	}

	/**
	 * Starts monitoring for the current test before it executes.
	 *
	 * @param context the current extension context.
	 * @throws Exception if the pre-processing fails.
	 */
	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		preProcess(context);
	}

	/**
	 * Finishes monitoring for the current test after it executes.
	 *
	 * @param context the current extension context.
	 * @throws Exception if the post-processing fails.
	 */
	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		postProcess(context);
	}
	
	/**
	 * Restores a fallback test session after the test class lifecycle ends.
	 *
	 * @param context the current extension context.
	 * @throws Exception if the cleanup fails.
	 */
	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		setActiveContext(createTestSession(systemUTC().instant()).createCallback()); //fake session, avoid no active session
	}

	/**
	 * Records a disabled test without executing its body.
	 *
	 * @param context the current extension context.
	 * @param reason the optional reason why the test is disabled.
	 */
	@Override
	public void testDisabled(ExtensionContext context, Optional<String> reason) {
		preProcess(context);
		postProcess(context);
	}
	
	static void preProcess(ExtensionContext context)  { //cannot check existing handler, see beforeAll
		updateExecutionListener(context, hndl-> traceAroundMethod(createTestSession(systemUTC().instant()), ses-> { 
			ses.setName(context.getDisplayName());
			ses.setLocation(context.getRequiredTestClass().getName(), context.getRequiredTestMethod().getName());
			//set test user
		}));
	}
	
	static void postProcess(ExtensionContext context){
		var end = systemUTC().instant();
		updateExecutionListener(context, hndl-> {
			if(assertMonitorNonNull(hndl, "Junit5TestMonitor.postProcess")) {
				hndl.safeHandle(null, end, null, context.getExecutionException().orElse(null));
			}
			return null;
		});
	}
	
	@SuppressWarnings("unchecked")
	static ExecutionListener<Void> updateExecutionListener(ExtensionContext context, UnaryOperator<ExecutionListener<Void>> op) {
		var str = context.getStore(NAMESPACE);
		var ses = op.apply(str.get(SESSION_KEY, ExecutionListener.class));
		str.put(SESSION_KEY, ses);
		return ses;
	}
}
