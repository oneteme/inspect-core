package org.usf.inspect.naming;

import static org.usf.inspect.naming.DirContextTracker.context;

import javax.naming.directory.DirContext;

import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.ContextSource;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class ContextSourceWrapper implements ContextSource  {

	private final ContextSource contextSource;

	@Override
	public DirContext getReadOnlyContext() throws NamingException {
		return context(contextSource::getReadOnlyContext);
	}

	@Override
	public DirContext getReadWriteContext() throws NamingException {
		return context(contextSource::getReadWriteContext);
	}
	
	@Override
	public DirContext getContext(String principal, String credentials) throws NamingException {
		return context(()-> contextSource.getContext(principal, credentials));
	}
	
	public static ContextSourceWrapper wrap(ContextSource contextSource) {
		return new ContextSourceWrapper(contextSource);
	}
}
