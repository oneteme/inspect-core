package org.usf.inspect.test;

import static java.time.Instant.now;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.SessionManager.createTestSession;
import static org.usf.inspect.core.SessionManager.releaseSession;
import static org.usf.inspect.core.SessionManager.setCurrentSession;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.MainSessionCallback;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
public final class TestSessionJunitMonitor {

	@Getter
	MainSessionCallback call;
	
	public TestSessionJunitMonitor preProcess(ExtensionContext context){
		var ses = createTestSession(now());
		call(()->{
			ses.setName(context.getDisplayName());
			ses.setLocation(context.getRequiredTestClass().getName(), context.getRequiredTestMethod().getName());
		});
		ses.emit();
		call = ses.createCallback();
		setCurrentSession(call);
		return this;
	}
	
	public TestSessionJunitMonitor postProcess(ExtensionContext context){
		var now = now();
		call(()->{
			context.getExecutionException()
				.map(ExceptionInfo::fromException)
				.ifPresent(call::setException);
			call.setEnd(now);
		});
		call.emit();
		releaseSession(call);
		return this;
	}
}
