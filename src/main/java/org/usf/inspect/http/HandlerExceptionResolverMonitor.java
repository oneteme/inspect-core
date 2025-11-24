package org.usf.inspect.http;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.http.HttpSessionMonitor.requireHttpMonitor;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

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
			var mnt = requireHttpMonitor(request);
			if(nonNull(mnt)) { //non filtered requests
				var ses = mnt.getSession();
				if(isNull(ses.getException())) {
					ses.setException(fromException(ex));
				}
			}
		}
		return null;
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}
}
