package org.usf.inspect.dir;

import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.InspectContext.context;

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
public final class ContextSourceWrapper implements ContextSource {

	private final ContextSource contextSource;

	@Override
	public DirContext getReadOnlyContext() throws NamingException {
		return new DirContextWrapper(contextSource::getReadOnlyContext);
	}

	@Override
	public DirContext getReadWriteContext() throws NamingException {
		return new DirContextWrapper(contextSource::getReadWriteContext);
	}
	
	@Override
	public DirContext getContext(String principal, String credentials) throws NamingException {
		return new DirContextWrapper(()-> contextSource.getContext(principal, credentials));
	}
	
	public static ContextSource wrap(ContextSource ctx) {
		if(context().getConfiguration().isEnabled()){
			logWrappingBean("contextSource", ctx.getClass());
			return new ContextSourceWrapper(ctx);
		}
		return ctx;
	}
}
