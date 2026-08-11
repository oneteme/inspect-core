package org.usf.inspect.test;

import static java.time.Clock.systemUTC;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.Monitor.traceAroundMethod;
import static org.usf.inspect.core.SessionContextManager.createTestSession;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import lombok.RequiredArgsConstructor;

/**
 * Monitors JUnit 4 test execution and records test lifecycle events for Inspect.
 */
@RequiredArgsConstructor
public final class Junit4TestMonitor implements TestRule {
	
	/**
	 * Wraps a JUnit 4 statement to trace the execution of the current test.
	 *
	 * @param base the original statement to execute.
	 * @param dscr the description of the current test.
	 * @return the statement wrapped with Inspect monitoring.
	 */
	@Override
	public Statement apply(Statement base, Description dscr) {
		return new Statement() {
			/**
			 * Evaluates the wrapped test statement while tracing the test execution.
			 *
			 * @throws Throwable if the wrapped statement fails.
			 */
			@Override
			public void evaluate() throws Throwable {
				exec(base::evaluate, traceAroundMethod(createTestSession(systemUTC().instant()), ses-> {
					ses.setName(dscr.getDisplayName());
					ses.setLocation(dscr.getClassName(), dscr.getMethodName());
					//set test user
				}));
			}
		};
	}
}
