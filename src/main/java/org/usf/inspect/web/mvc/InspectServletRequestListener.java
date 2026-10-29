package org.usf.inspect.web.mvc;

import static java.lang.Boolean.TRUE;
import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.web.mvc.HttpSessionTracer.httpSessionTracer;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class InspectServletRequestListener implements ServletRequestListener {

	
	static final String SESSION_TRACER = "inspect-http-session-tracer";
	static final String SESSION_IGNORE = "inspect-http-session-ignore"; 

	private final HttpRoutePredicate routePredicate;
	
	@Override
	public void requestInitialized(ServletRequestEvent sre) {
		var sr = sre.getServletRequest();
		if(sr instanceof HttpServletRequest req && isNull(currentSessionTracer(req))) {
			if(routePredicate.accept(req)) {
				req.setAttribute(SESSION_TRACER, httpSessionTracer(req));
			}
			else {
				log.debug("request {} ignored by route predicate", req.getRequestURI());
				req.setAttribute(SESSION_IGNORE, TRUE);
			}
		}
	}
	
	@Override
	public void requestDestroyed(ServletRequestEvent sre) {
		if(sre.getServletRequest() instanceof HttpServletRequest req && !isSessionIgnored(req)) {
			var trc = requireSessionTracer(req, "InspectServletRequestListener.requestDestroyed");
			if(nonNull(trc)) {
				trc.safeHandle(null, systemUTC().instant(), null, null);
				sre.getServletRequest().removeAttribute(SESSION_TRACER);
			}
		}
	}

	static HttpSessionTracer currentSessionTracer(HttpServletRequest req) {
		return (HttpSessionTracer) req.getAttribute(SESSION_TRACER);
	}
	
	static boolean isSessionIgnored(HttpServletRequest req) {
		return TRUE.equals(req.getAttribute(SESSION_IGNORE));
	}

	static HttpSessionTracer requireSessionTracer(HttpServletRequest req, String action) {
		var trc = currentSessionTracer(req);
		if(isNull(trc)) {
			REPORTER.warn("HttpSessionFilter.{}: no session tracer found for request {}", action, req.getRequestURI());
		}
		return trc;
	}
}
