package org.usf.inspect.test;

import static java.time.Instant.now;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.Monitor.traceAroundMethod;
import static org.usf.inspect.core.SessionContextManager.createTestSession;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class Junit4TestMonitor implements TestRule {
	
	@Override
	public Statement apply(Statement base, Description dscr) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				exec(base::evaluate, traceAroundMethod(createTestSession(now()), ses-> {
					ses.setName(dscr.getDisplayName());
					ses.setLocation(dscr.getClassName(), dscr.getMethodName());
					//set test user
				}));
			}
		};
	}
}
