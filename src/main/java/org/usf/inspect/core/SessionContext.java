package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.MainSessionType.STARTUP;
import static org.usf.inspect.core.SessionManager.releaseSession;
import static org.usf.inspect.core.SessionManager.releaseStartupSession;
import static org.usf.inspect.core.SessionManager.setCurrentSession;
import static org.usf.inspect.core.SessionManager.setStartupSession;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SessionContext {
	
	final AbstractSession2 session;
	final AbstractSessionCallback callback;

	public boolean wasCompleted() {
		return nonNull(callback);
	}
	
	public void setup() {
		if(session instanceof MainSession2 msc && STARTUP.name().equals(msc.getType())) {//TODO check type
			setStartupSession(callback);
		}
		else {
			setCurrentSession(callback);
		}
	}
	
	public void release() {
		if(session instanceof MainSession2 msc && STARTUP.name().equals(msc.getType())) {
			releaseStartupSession(callback);
		}
		else {
			releaseSession(callback);
		}
	}
}
