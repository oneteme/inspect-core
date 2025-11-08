package org.usf.inspect.test;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.usf.inspect.core.ErrorReporter.reportMessage;
import static org.usf.inspect.core.SessionManager.createTestSession;

import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestWatcher;
import org.usf.inspect.core.SessionManager;

/**
 * 
 * @author u$f
 *
 */
public final class JunitTestWatcher implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, TestWatcher, AfterAllCallback {

	private static final Namespace NAMESPACE = create(JunitTestWatcher.class.getName());
	private static final String SESSION_KEY = "inspect-hook";
	
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		createTestSession().updateContext(); //fake session, avoid no session
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		context.getStore(NAMESPACE).put(SESSION_KEY, new TestSessionJunitMonitor().preProcess(context));
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		var mnt = context.getStore(NAMESPACE).get(SESSION_KEY, TestSessionJunitMonitor.class);
		if(nonNull(mnt)) {
			mnt.postProcess(context);
		}
		else {
			reportMessage("JunitTestWatcher.afterEach", null, "session is null");
		}
	}
	
	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		createTestSession().updateContext(); //fake session, avoid no session
	}

	@Override
	public void testDisabled(ExtensionContext context, Optional<String> reason) {
		new TestSessionJunitMonitor()
		.preProcess(context)
		.postProcess(context);
	}
}
