package org.usf.inspect.test;

import static java.time.Instant.now;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.SessionContextManager.createTestSession;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.MainSessionCallback;
import org.usf.inspect.core.SessionContext;

/**
 * 
 * @author u$f
 *
 */
public final class TestSessionJunitMonitor {

	private SessionContext ctx;
	private MainSessionCallback call;
	
	public TestSessionJunitMonitor preProcess(ExtensionContext context){
		var ses = createTestSession(now());
		call(()->{
			ses.setName(context.getDisplayName());
			ses.setLocation(context.getRequiredTestClass().getName(), context.getRequiredTestMethod().getName());
			ses.emit();
		});
		call = ses.createCallback();
		ctx = call.setupContext();
		return this;
	}
	
	public TestSessionJunitMonitor postProcess(ExtensionContext context){
		var now = now();
		call(()->{
			context.getExecutionException()
				.map(ExceptionInfo::fromException)
				.ifPresent(call::setException);
			call.setEnd(now);
			call.emit();
		});
		ctx.release();
		return this;
	}
}
