package org.usf.inspect.test;

import static java.time.Clock.systemUTC;
import static org.usf.inspect.core.ExecutionTracer.forMainSession;
import static org.usf.inspect.core.InspectExecutor.exec;
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
				exec(base::evaluate, forMainSession(()-> {
					var sgn = createTestSession(systemUTC().instant());
					sgn.setName(dscr.getDisplayName());
					sgn.setLocation(dscr.getClassName(), dscr.getMethodName());
					//set test user
					return sgn;
				}));
			}
		};
	}
}
