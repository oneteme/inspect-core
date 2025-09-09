package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.usf.inspect.http.HttpRoutePredicate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public class HandlerExceptionResolverMonitor implements HandlerExceptionResolver, Ordered {
	
	private final HttpRoutePredicate routePredicate;
	
	/**
	 * Filter → Interceptor.preHandle → Controller → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
	 * 
	 * @return {@code null} for default processing in the resolution chain
	 */
	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		if(routePredicate.accept(request)) {
			var ses = requireCurrentSession(RestSession.class);
			if(nonNull(ses) && isNull(ses.getException())) { //non filtered requests
				ses.runSynchronized(()-> ses.setException(fromException(ex)));
			}
		}
		return null;
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}
}
