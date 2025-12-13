package org.usf.inspect.test;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.createTestSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.usf.inspect.core.Monitor;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class Junit4TestMonitor implements TestRule, Monitor {
	
	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				var call = createTestSession(now(), ses->{
					ses.setName(description.getDisplayName());
					ses.setLocation(description.getClassName(), description.getMethodName());
				});
				setActiveContext(call);
				exec(base::evaluate, (s,e,m,t)-> {
					if(assertStillOpened(call)) { //report if session was closed
						call.setStart(s);
						if(nonNull(t)) {
							call.setException(fromException(t));
						}
						call.setEnd(e);
						emit(call);
					}
					clearContext(call);
				});
			}
		};
	}
}
