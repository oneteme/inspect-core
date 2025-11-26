package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.SessionContextManager.clearContext;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class SessionContext {
	
	final boolean startup;
	@NonNull final AbstractSessionCallback callback;
	
	public String getId() {
		return callback.getId();
	}

	public SessionContext setup() {
		setActiveContext(this);
		return this;
	}
	
	public SessionContext release() {
		clearContext(this);
		return this;
	}

	public boolean wasCompleted() {
		return nonNull(callback.getEnd()) && !callback.isAsync();
	}
}
