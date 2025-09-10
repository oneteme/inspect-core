package org.usf.inspect.test;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.usf.inspect.core.ErrorReporter.reportMessage;

import java.util.Optional;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * 
 * @author u$f
 *
 */
public final class JunitTestWatcher implements BeforeEachCallback, AfterEachCallback, TestWatcher {

	private static final Namespace NAMESPACE = create(JunitTestWatcher.class.getName());
	private static final String SESSION_KEY = "$monitor";

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		var mnt = new TestSessionJunitMonitor();
		mnt.preProcess(context);
		context.getStore(NAMESPACE).put(SESSION_KEY, mnt);
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
	public void testDisabled(ExtensionContext context, Optional<String> reason) {
		var mnt = new TestSessionJunitMonitor();
		mnt.preProcess(context);
		mnt.postProcess(context);
	}
}
