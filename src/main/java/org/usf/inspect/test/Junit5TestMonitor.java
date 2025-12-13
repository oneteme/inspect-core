package org.usf.inspect.test;

import static java.time.Instant.now;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.usf.inspect.core.Monitor.assertMonitorNonNull;
import static org.usf.inspect.core.SessionContextManager.createTestSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;

import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestWatcher;

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
		setActiveContext(createTestSession(now(), ses->{})); //fake session, avoid no session
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		context.getStore(NAMESPACE).put(SESSION_KEY, new TestSessionJunitMonitor().preProcess(context));
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		var mnt = context.getStore(NAMESPACE).get(SESSION_KEY, TestSessionJunitMonitor.class);
		if(assertMonitorNonNull(mnt, "Junit5TestMonitor.afterEach")) {
			mnt.postProcess(context);
		}
	}
	
	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		setActiveContext(createTestSession(now(), ses->{})); //fake session, avoid no session
	}

	@Override
	public void testDisabled(ExtensionContext context, Optional<String> reason) {
		new TestSessionJunitMonitor()
		.preProcess(context)
		.postProcess(context);
	}
}
