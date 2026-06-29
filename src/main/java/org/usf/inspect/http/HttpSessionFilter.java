package org.usf.inspect.http;

import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.inspect.core.Helper.evalExpression;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.Monitor.assertMonitorNonNull;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.usf.inspect.core.HttpUserProvider;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.TraceableStage;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletException;

/**
 * 
 * @author u$f 
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class HttpSessionFilter extends OncePerRequestFilter implements HandlerInterceptor {

	static final String SESSION_MONITOR = "inspect-http-request-monitor";
	static final Collector<CharSequence, ?, String> joiner = joining("_");

	private final HttpRoutePredicate routePredicate;
	private final HttpUserProvider userProvider;
	
	@Override
	protected void doFilterInternal(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse res, javax.servlet.FilterChain filterChain) throws IOException, ServletException {
//		var cRes = new ContentCachingResponseWrapper(res) doesn't works with async
		try {
			exec(()-> filterChain.doFilter(req, res), filterHandler(req, res));	
		}
		catch (IOException | javax.servlet.ServletException | RuntimeException e) {
			throw e;
		}
		catch (Exception e) {//should never happen
			hub().reportError(false, "HttpSessionFilter.doFilterInternal", e);
			throw new IllegalStateException(e); 
		}
	}
	
	private ExecutionListener<Void> filterHandler(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse res) {
		var mnt = currentHttpMonitor(req);
		if(isNull(mnt)) {
			mnt = new HttpSessionMonitor(req, res);
			req.setAttribute(SESSION_MONITOR, mnt);
		}
		return mnt.preFilter(()-> this.isAsyncStarted(req));
	}

	@Override
	protected boolean shouldNotFilter(javax.servlet.http.HttpServletRequest request) throws javax.servlet.ServletException {
		return !routePredicate.accept(request);
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() { //Callable | Differed | @Async
		return false;
	}
	
	@Override
	public boolean preHandle(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, Object handler) throws Exception {
		if(shouldIntercept(handler)) {  //avoid unfiltred request
			var mnt = currentHttpMonitor(request);
			if(assertMonitorNonNull(mnt, "HttpSessionFilter.preHandle")) {
				mnt.preProcess();
			}
		}
		return HandlerInterceptor.super.preHandle(request, response, handler);
	}
	
	@Override
	public void postHandle(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		if(shouldIntercept(handler)) { //avoid unfiltred request
			var mnt = currentHttpMonitor(request);
			if(assertMonitorNonNull(mnt, "HttpSessionFilter.postHandle")) {
				mnt.process();
			}
		}
	}

	@Override
	public void afterCompletion(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, Object handler, Exception ex) throws Exception {
		if(shouldIntercept(handler)) { //avoid unfiltred request 
			var mnt = currentHttpMonitor(request);
			if(assertMonitorNonNull(mnt, "HttpSessionFilter.afterCompletion")) {
				var name = resolveEndpointName(handler, request);
				var user = userProvider.getUser(request, name);
				mnt.postProcess(name, user, ex);
			}
		}
	}
	
	private String resolveEndpointName(Object handler, javax.servlet.http.HttpServletRequest req) {
		if(handler instanceof HandlerMethod mth) {
			var ant = mth.getMethodAnnotation(TraceableStage.class);
			if(nonNull(ant) && !ant.name().isEmpty()) {
				try {
					return evalExpression(ant.name(), 
							mth.getBean(), mth.getBeanType(), 
							new String[] {"request"}, new Object[] {req}).toString();
				}
				catch (Exception e) {
					log.warn("cannot eval expression ='{}' on {}.{}", 
							ant.name(), mth.getBeanType().getSimpleName(), mth.getMethod().getName());
				}
			}
		}
		return defaultEndpointName(req);
	}
	
	@SuppressWarnings("unchecked")
	private static String defaultEndpointName(javax.servlet.http.HttpServletRequest req) {
		var arr = req.getRequestURI().substring(1).split("/");
		var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		return isNull(map) ? join("_", arr) : Stream.of(arr)
				.filter(not(map.values()::contains))
				.collect(joiner);
	}

	static boolean shouldIntercept(Object handler) {  //BasicErrorController 
		return handler instanceof HandlerMethod mth && 
				!(mth.getBean() instanceof ErrorController);
	}
    
    static HttpSessionMonitor currentHttpMonitor(javax.servlet.http.HttpServletRequest req) {
    	return (HttpSessionMonitor) req.getAttribute(SESSION_MONITOR);
    }
}
