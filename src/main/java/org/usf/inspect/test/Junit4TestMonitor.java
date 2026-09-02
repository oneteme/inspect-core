package org.usf.inspect.test;

import static org.usf.inspect.core.InspectExecutor.exec;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.usf.inspect.core.SessionExecutionListener;

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
		var listener = new SessionExecutionListener();
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				exec(base::evaluate, listener.executionListener(sgn-> {
					sgn.setName(dscr.getDisplayName());
					sgn.setLocation(dscr.getClassName(), dscr.getMethodName());
					//set test user
				}));
			}
		};
	}
}
