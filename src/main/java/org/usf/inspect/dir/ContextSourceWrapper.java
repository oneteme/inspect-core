package org.usf.inspect.dir;

import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import javax.naming.directory.DirContext;

import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.ContextSource;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
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
		return wrap(ctx, null);
	}
	
	public static ContextSource wrap(ContextSource ctx, String beanName) {
		if(hub().getConfiguration().isEnabled()){
			if(ctx.getClass() != ContextSourceWrapper.class) {
				logWrappingBean(requireNonNullElse(beanName, "contextSource"), ctx.getClass());
				return new ContextSourceWrapper(ctx);
			}
			else {
				log.warn("{}: {} is already wrapped", beanName, ctx);
			}
		}
		return ctx;
	}
}
