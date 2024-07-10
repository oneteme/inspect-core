package org.usf.inspect.naming;

import static org.usf.inspect.naming.DirContextTracker.connect;

import javax.naming.directory.DirContext;

import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.ContextSource;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class ContextSourceWrapper implements ContextSource  {

	@Delegate
	private final ContextSource contextSource;

	public DirContext getReadOnlyContext() throws NamingException {
		return connect(contextSource::getReadOnlyContext);
	}

	public DirContext getReadWriteContext() throws NamingException {
		return connect(contextSource::getReadWriteContext);
	}
	
	public static ContextSourceWrapper wrap(ContextSource contextSource) {
		return new ContextSourceWrapper(contextSource);
	}
}
