package org.usf.inspect.test;

import static java.time.Instant.now;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.createTestSession;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.MainSession;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
public final class TestSessionJunitMonitor {

	@Getter
	final MainSession main = createTestSession();
	
	public void preProcess(ExtensionContext context){
		var now = now();
		call(()->{
			main.setStart(now);
			main.setThreadName(threadName());
			main.setName(context.getDisplayName());
			main.setLocation(context.getRequiredTestClass().getName(), context.getRequiredTestMethod().getName());
			main.updateContext().emit();
		});
	}
	
	public void postProcess(ExtensionContext context){
		var now = now();
		call(()->{
			main.runSynchronized(()->{
				context.getExecutionException()
					.map(ExceptionInfo::fromException)
					.ifPresent(main::setException);
				main.setEnd(now);
			});
			main.releaseContext();
		});
	}
}
