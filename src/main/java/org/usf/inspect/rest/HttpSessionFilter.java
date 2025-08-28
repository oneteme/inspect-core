package org.usf.inspect.rest;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.inspect.core.ErrorReporter.reportError;
import static org.usf.inspect.core.ErrorReporter.reporter;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.evalExpression;
import static org.usf.inspect.core.HttpAction.DEFERRED;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.SessionManager.currentSession;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.HttpUserProvider;
import org.usf.inspect.core.TraceableStage;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 
 * @author u$f 
 *
 */
public final class HttpSessionFilter extends OncePerRequestFilter implements HandlerInterceptor {

	static final String SESSION_MONITOR = HttpSessionFilter.class.getName() + ".monitor";
	static final String TRACE_HEADER = "x-tracert"; //"x-inspect"

	static final Collector<CharSequence, ?, String> joiner = joining("_");

	private final RoutePredicate routePredicate;
	private final HttpUserProvider userProvider;

	public HttpSessionFilter(RoutePredicate routePredicate, HttpUserProvider userProvider) {
		this.routePredicate = routePredicate;
		this.userProvider = userProvider;
	}
	
	@Override
	protected boolean shouldNotFilterAsyncDispatch() { //Callable | Differed | @Async
		return false;
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
		traceRestSession(req, res);
//		var cRes = new ContentCachingResponseWrapper(res) doesn't works with async
		try {
			exec(()-> filterChain.doFilter(req, res), restSessionListener(req, res));	
		}
		catch (IOException | ServletException | RuntimeException e) {
			throw e;
		}
		catch (Exception e) {//should never happen
			reportError("FilterExecutionMonitor.doFilterInternal", null, e);
			throw new IllegalStateException(e); 
		}
	}
	
	private void traceRestSession(HttpServletRequest req, HttpServletResponse res) {
		var mnt = (HttpSessionMonitor) req.getAttribute(SESSION_MONITOR);
		if(isNull(mnt)) {
			mnt = new HttpSessionMonitor(req, req.getHeader(TRACE_HEADER));
			res.addHeader(TRACE_HEADER, mnt.getSession().getId()); //add headers before doFilter
			res.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
			req.setAttribute(SESSION_MONITOR, mnt);
		}
		else {
			mnt.getSession().updateContext(); //deferred execution
		}
	}
	
	private ExecutionMonitorListener<Void> restSessionListener(HttpServletRequest request, HttpServletResponse response) {
		return (s,e,o,t)-> {
			var mnt = (HttpSessionMonitor) request.getAttribute(SESSION_MONITOR);
			if(nonNull(mnt)) {
				if(!isAsyncStarted(request)) { //!Async || isAsyncDispatch
					return mnt.handleDisconnection(e, response, t);
				}
				else {
					mnt.asyncStageHandler(DEFERRED);
					if(nonNull(t)) {
						return mnt.handleDisconnection(e, response, t);
					}
				}
			}
			else {
				reporter().action("restSessionListener").message("HttpSessionMonitor is null").emit();
			}
			return null; //do not trace this
		};
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return !routePredicate.accept(request);
	}

	/**
	 * Filter → Interceptor.preHandle → Controller → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		var mnt = (HttpSessionMonitor) request.getAttribute(SESSION_MONITOR);
		if(nonNull(mnt) && shouldIntercept(handler)) {  //avoid unfiltered request
			mnt.asyncStageHandler(PRE_PROCESS);
		}
		return HandlerInterceptor.super.preHandle(request, response, handler);
	}
	
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		var mnt = (HttpSessionMonitor) request.getAttribute(SESSION_MONITOR);
		if(nonNull(mnt) && shouldIntercept(handler)) { //avoid unfiltered request
			mnt.asyncStageHandler(PROCESS);
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		var mnt = (HttpSessionMonitor) request.getAttribute(SESSION_MONITOR);
		if(nonNull(mnt) && shouldIntercept(handler)) { //avoid unfiltered request 
			mnt.asyncStageHandler(POST_PROCESS);
			mnt.handleAfterComplete(resolveEndpointName(handler, request), userProvider, ex);
		}
	}
	
	private String resolveEndpointName(Object handler, HttpServletRequest req) {
		if(handler instanceof HandlerMethod mth) {
			var ant = mth.getMethodAnnotation(TraceableStage.class);
			if(nonNull(ant) && !ant.name().isEmpty()) {
				try {
					return evalExpression(ant.name(), 
							mth.getBean(), mth.getBeanType(), 
							new String[] {"request"}, new Object[] {req}).toString();
				}
				catch (Exception e) {
					reporter().action("resolveEndpointName")
					.message(format("eval expression '%s' on %s.%s", 
							ant.name(), mth.getBeanType().getSimpleName(), mth.getMethod().getName()))
					.cause(e).trace(currentSession()).emit();
				}
			}
		}
		return defaultEndpointName(req);
	}

	@SuppressWarnings("unchecked")
	private static String defaultEndpointName(HttpServletRequest req) {
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
}