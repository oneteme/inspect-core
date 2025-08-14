package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionManager.createTestSession;
import static org.usf.inspect.core.SessionManager.emitSessionEnd;
import static org.usf.inspect.core.SessionManager.emitSessionStart;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class JunitTestMonitor implements BeforeEachCallback, AfterEachCallback {

	private static final ExtensionContext.Namespace NAMESPACE = create(JunitTestMonitor.class.getName());
	private static final String SESSION_KEY = "$session";
	
	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		var now = now();
		var main = createTestSession();
		try {
			main.setStart(now);
			main.setName(context.getRequiredTestMethod().getName() + "#" + context.getDisplayName());
			main.setLocation(context.getRequiredTestClass().getName());
			main.setThreadName(threadName());
	        context.getStore(NAMESPACE).put(SESSION_KEY, main);
		}
		catch (Exception e) {
			context().reportEventHandleError(main.getId(), e);
		}
		emitSessionStart(main);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
        var main = context.getStore(NAMESPACE).get(SESSION_KEY, MainSession.class);
        if(nonNull(main)) {
        	try {
                main.setEnd(now());
                context.getExecutionException()
                .ifPresent(e-> main.setException(fromException(e)));
        	}
        	catch (Exception e) {
    			context().reportEventHandleError(main.getId(), e);
			}
        	emitSessionEnd(main);
        }
	}
}
