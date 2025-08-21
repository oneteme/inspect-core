package org.usf.inspect.core;

import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.createTestSession;
import static org.usf.inspect.core.SessionManager.emitSession;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class JUnit4TestMonitor implements TestRule  {

	@Override
	public Statement apply(Statement base, Description description) {
		var main = createTestSession();
		return call(()-> wrapStatement(base), (s,e,m,t)->{
			main.setThreadName(threadName());
			main.setStart(s);
			main.setEnd(e);
			main.setName(description.getDisplayName());
			main.setLocation(description.getClassName(), description.getMethodName());
			main.setException(fromException(t));
			emitSession(main);
		});
	}

	Statement wrapStatement(Statement base) {
		return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
            }
        };
	}
}
