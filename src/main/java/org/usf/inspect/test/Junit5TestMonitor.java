package org.usf.inspect.test;

import static java.time.Instant.now;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.usf.inspect.core.ExecutionMonitor.notifyHandler;
import static org.usf.inspect.core.Monitor.assertMonitorNonNull;
import static org.usf.inspect.core.Monitor.mainExecutionHandler;
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
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;

/**
 * 
 * @author u$f
 *
 */
public final class Junit5TestMonitor implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, TestWatcher, AfterAllCallback {

	private static final Namespace NAMESPACE = create(Junit5TestMonitor.class.getName());
	private static final String SESSION_KEY = "inspect-junit-monitor";
	
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		setActiveContext(createTestSession(now()).createCallback()); //fake session, avoid no active session
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		preProcess(context);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		postProcess(context);
	}
	
	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		setActiveContext(createTestSession(now()).createCallback()); //fake session, avoid no active session
	}

	@Override
	public void testDisabled(ExtensionContext context, Optional<String> reason) {
		preProcess(context);
		postProcess(context);
	}
	
	static void preProcess(ExtensionContext context)  { //cannot check existing handler, see beforeAll
		updateStoredSession(context, hndl-> mainExecutionHandler(createTestSession(now()), ses-> { 
			ses.setName(context.getDisplayName());
			ses.setLocation(context.getRequiredTestClass().getName(), context.getRequiredTestMethod().getName());
			//set user
		}));
	}
	
	static void postProcess(ExtensionContext context){
		var now = now();
		updateStoredSession(context, hndl-> {
			if(assertMonitorNonNull(hndl, "Junit5TestMonitor.postProcess")) {
				notifyHandler(hndl, null, now, null, context.getExecutionException().orElse(null));
			}
			return null;
		});
	}
	
	@SuppressWarnings("unchecked")
	static ExecutionHandler<Void> updateStoredSession(ExtensionContext context, UnaryOperator<ExecutionHandler<Void>> op) {
		var str = context.getStore(NAMESPACE);
		var ses = op.apply(str.get(SESSION_KEY, ExecutionHandler.class));
		str.put(SESSION_KEY, ses);
		return ses;
	}
}
