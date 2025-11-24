package org.usf.inspect.http;

import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.inspect.core.ErrorReporter.reportError;
import static org.usf.inspect.core.ErrorReporter.reportMessage;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.evalExpression;
import static org.usf.inspect.core.SessionContextManager.currentSession;
import static org.usf.inspect.http.HttpSessionMonitor.SESSION_MONITOR;
import static org.usf.inspect.http.HttpSessionMonitor.currentHttpMonitor;
import static org.usf.inspect.http.HttpSessionMonitor.requireHttpMonitor;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.HttpUserProvider;
import org.usf.inspect.core.TraceableStage;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f 
 *
 */
@Slf4j
public final class HttpSessionFilter extends OncePerRequestFilter implements HandlerInterceptor {

	static final Collector<CharSequence, ?, String> joiner = joining("_");

	private final HttpRoutePredicate routePredicate;
	private final HttpUserProvider userProvider;

	public HttpSessionFilter(HttpRoutePredicate routePredicate, HttpUserProvider userProvider) {
		this.routePredicate = routePredicate;
		this.userProvider = userProvider;
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
		traceRestSession(req, res);
//		var cRes = new ContentCachingResponseWrapper(res) doesn't works with async
		try {
			exec(()-> filterChain.doFilter(req, res), filterHandler(req, res));	
		}
		catch (IOException | ServletException | RuntimeException e) {
			throw e;
		}
		catch (Exception e) {//should never happen
			reportError("FilterExecutionMonitor.doFilterInternal", currentSession(), e);
			throw new IllegalStateException(e); 
		}
	}
	
	private void traceRestSession(HttpServletRequest req, HttpServletResponse res) {
		var mnt = currentHttpMonitor(req);
		if(isNull(mnt)) {
			mnt = new HttpSessionMonitor();
			mnt.preFilter(req, req.getHeader(TRACE_HEADER)); //called once
			res.addHeader(TRACE_HEADER, mnt.getSession().getId()); //add headers before doFilter
			res.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
		}
		else {
			mnt.getSession().updateContext(); //deferred execution
		}
	}
	
	private ExecutionHandler<Void> filterHandler(HttpServletRequest req, HttpServletResponse res) {
		
		var mnt = currentHttpMonitor(req);
		if(isNull(mnt)) {
			mnt = new HttpSessionMonitor();
			mnt.preFilter(req, res); //called once
			req.setAttribute(SESSION_MONITOR, mnt);
		}
		else {
			mnt.getSession().updateContext(); //deferred execution
		}
		return (s,e,o,t)-> {
			var mnt = requireHttpMonitor(req);
			if(nonNull(mnt)) {
				mnt.postFilterHandler(isAsyncStarted(req), e, response, t);
			}
			else {
				reportMessage("restSessionListener", null, null);
			}
		};
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return !routePredicate.accept(request);
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() { //Callable | Differed | @Async
		return false;
	}
	
	/**
	 * Filter → Interceptor.preHandle → Controller → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		var mnt = requireHttpMonitor(request);
		if(nonNull(mnt) && shouldIntercept(handler)) {  //avoid unfiltered request
			mnt.preProcess();
		}
		return HandlerInterceptor.super.preHandle(request, response, handler);
	}
	
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		var mnt = requireHttpMonitor(request);
		if(nonNull(mnt) && shouldIntercept(handler)) { //avoid unfiltered request
			mnt.process();
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		var mnt = requireHttpMonitor(request);
		if(nonNull(mnt) && shouldIntercept(handler)) { //avoid unfiltered request 
			var name = resolveEndpointName(handler, request);
			var user = userProvider.getUser(request, name);
			mnt.postProcess(name, user, ex);
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
					log.warn("cannot eval expression ='%s' on %s.%s", 
							ant.name(), mth.getBeanType().getSimpleName(), mth.getMethod().getName());
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