package org.usf.inspect.http;

import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.http.HttpSessionFilter.currentSessionTracer;
import static org.usf.inspect.http.HttpSessionFilter.requireSessionTracer;
import static org.usf.inspect.http.HttpSessionTracer.httpSessionTracer;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 
 * @author u$f
 *
 */
public final class InspectServletRequestListener implements ServletRequestListener {

	static final String SESSION_TRACER = "inspect-http-session-tracer";
	
	@Override
	public void requestInitialized(ServletRequestEvent sre) {
		var sr = sre.getServletRequest();
		if(sr instanceof HttpServletRequest req && isNull(currentSessionTracer(req))) {
			req.setAttribute(SESSION_TRACER, httpSessionTracer(req));
		}
	}
	
	@Override
	public void requestDestroyed(ServletRequestEvent sre) {
		if(sre.getServletRequest() instanceof HttpServletRequest req) {
			var trc = requireSessionTracer(req, "HttpAsyncExecutionTracer.onComplete");
			if(nonNull(trc)) {
				trc.safeHandle(null, systemUTC().instant(), null, null);
				sre.getServletRequest().removeAttribute(SESSION_TRACER);
			}
		}
	}
}
