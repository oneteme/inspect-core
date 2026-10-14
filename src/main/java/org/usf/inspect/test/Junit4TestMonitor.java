package org.usf.inspect.test;

import static java.time.Clock.systemUTC;
import static org.usf.inspect.core.Constants.JUNIT4_ENABLED;
import static org.usf.inspect.core.ExecutionTracer.forMainSession;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.MainSessionType.TEST;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * 
 * @author u$f
 *
 */
public final class Junit4TestMonitor implements TestRule {
	
	@Override
	public Statement apply(Statement base, Description dscr) {
		if(getConfiguration().isEnabled(JUNIT4_ENABLED, true)) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					exec(base::evaluate, forMainSession(systemUTC().instant(), TEST, sgn-> {
						sgn.setName(dscr.getDisplayName());
						sgn.setLocation(formatLocation(dscr.getClassName(), dscr.getMethodName()));
						//set test user
					}));
				}
			};
		}
		return base;
	}
}
