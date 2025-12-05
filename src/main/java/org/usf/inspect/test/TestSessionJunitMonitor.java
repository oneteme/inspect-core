package org.usf.inspect.test;

import static java.time.Instant.now;
import static org.usf.inspect.core.Callback.assertStillOpened;
import static org.usf.inspect.core.ExecutionMonitor.runSafely;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.createTestSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.MainSessionCallback;

/**
 * 
 * @author u$f
 *
 */
public final class TestSessionJunitMonitor {

	private MainSessionCallback call;
	
	public TestSessionJunitMonitor preProcess(ExtensionContext context){
		var ses = createTestSession(now());
		runSafely(()->{
			ses.setName(context.getDisplayName());
			ses.setLocation(context.getRequiredTestClass().getName(), context.getRequiredTestMethod().getName());
			ses.emit();
		});
		call = ses.createCallback();
		setActiveContext(call);
		return this;
	}
	
	public TestSessionJunitMonitor postProcess(ExtensionContext context){
		var now = now();
		if(assertStillOpened(call)) { //report if session was closed
			runSafely(()->{
				context.getExecutionException()
					.map(ExceptionInfo::fromException)
					.ifPresent(call::setException);
				call.setEnd(now);
				call.emit();	
			});
			clearContext(call);
			call = null;
		}	
		return this;
	}
}
