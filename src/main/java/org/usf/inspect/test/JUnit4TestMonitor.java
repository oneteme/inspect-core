package org.usf.inspect.test;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.createTestSession;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * 
 * @author u$f
 *
 */
public final class JUnit4TestMonitor implements TestRule {

	@Override
	public Statement apply(Statement base, Description description) {
		var main = createTestSession();
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				call(()->{
					main.setStart(now());
					main.setThreadName(threadName());
					main.setName(description.getDisplayName());
					main.setLocation(description.getClassName(), description.getMethodName());
					main.updateContext().emit();
				});
				exec(base::evaluate, (s,e,m,t)-> {
					main.runSynchronized(()-> {
						main.setStart(s);
						if(nonNull(t)) {
							main.setException(fromException(t));
						}
						main.setEnd(e);
					});
					main.releaseContext();
				});
			}
		};
	}
}
