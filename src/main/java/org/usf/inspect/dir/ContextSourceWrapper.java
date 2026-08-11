package org.usf.inspect.dir;

import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import javax.naming.directory.DirContext;

import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.ContextSource;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Wraps a Spring LDAP {@link ContextSource} to monitor created directory contexts.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContextSourceWrapper implements ContextSource {

	private final ContextSource contextSource;

	/**
	 * Obtains a monitored read-only directory context.
	 *
	 * @return the monitored read-only directory context
	 * @throws NamingException if the context cannot be created
	 */
	@Override
	public DirContext getReadOnlyContext() throws NamingException {
		return new DirContextWrapper(contextSource::getReadOnlyContext);
	}

	/**
	 * Obtains a monitored read-write directory context.
	 *
	 * @return the monitored read-write directory context
	 * @throws NamingException if the context cannot be created
	 */
	@Override
	public DirContext getReadWriteContext() throws NamingException {
		return new DirContextWrapper(contextSource::getReadWriteContext);
	}
	
	/**
	 * Obtains a monitored directory context for the supplied credentials.
	 *
	 * @param principal the principal to authenticate with
	 * @param credentials the credentials to authenticate with
	 * @return the monitored directory context
	 * @throws NamingException if the context cannot be created
	 */
	@Override
	public DirContext getContext(String principal, String credentials) throws NamingException {
		return new DirContextWrapper(()-> contextSource.getContext(principal, credentials));
	}

	/**
	 * Wraps the supplied context source when tracing is enabled.
	 *
	 * @param ctx the context source to wrap
	 * @return the wrapped context source or the original instance
	 */
	public static ContextSource wrap(ContextSource ctx) {
		return wrap(ctx, null);
	}
	
	/**
	 * Wraps the supplied context source when tracing is enabled and it is not already wrapped.
	 *
	 * @param ctx the context source to wrap
	 * @param beanName the bean name associated with the context source
	 * @return the wrapped context source or the original instance
	 */
	public static ContextSource wrap(@NonNull ContextSource ctx, String beanName) {
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
