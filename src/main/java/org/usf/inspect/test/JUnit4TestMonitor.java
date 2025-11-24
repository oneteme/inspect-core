package org.usf.inspect.test;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.SessionManager.createTestSession;
import static org.usf.inspect.core.SessionManager.releaseSession;
import static org.usf.inspect.core.SessionManager.setCurrentSession;

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
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				var main = createTestSession(now());
				call(()->{
					main.setName(description.getDisplayName());
					main.setLocation(description.getClassName(), description.getMethodName());
					main.emit();
				});
				var call = main.createCallback();
				setCurrentSession(call);
				exec(base::evaluate, (s,e,m,t)-> {
					call.setStart(s);
					if(nonNull(t)) {
						call.setException(fromException(t));
					}
					call.setEnd(e);
					call.emit();
					releaseSession(call);
				});
			}
		};
	}
}
