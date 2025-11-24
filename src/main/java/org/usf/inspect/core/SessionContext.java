package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.SessionContextManager.releaseSession;
import static org.usf.inspect.core.SessionContextManager.releaseStartupSession;
import static org.usf.inspect.core.SessionContextManager.setCurrentSession;
import static org.usf.inspect.core.SessionContextManager.setStartupSession;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class SessionContext {
	
	final boolean startup;
	final AbstractSessionCallback callback;

	public SessionContext setup() {
		if(startup) {
			setStartupSession(this);
		}
		else {
			setCurrentSession(this);
		}
		return this;
	}
	
	public SessionContext release() {
		if(startup) {
			releaseStartupSession(this);
		}
		else {
			releaseSession(this);
		}
		return this;
	}

	public boolean wasCompleted() {
		return nonNull(callback);
	}
}
