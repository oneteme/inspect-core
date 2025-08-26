package org.usf.inspect.rest;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.net.URI.create;
import static java.time.Instant.now;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.USER_AGENT;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.evalExpression;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.HttpAction.DEFERRED;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionManager.currentSession;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.HttpRouteMonitoringProperties;
import org.usf.inspect.core.HttpUserProvider;
import org.usf.inspect.core.RestSession;
import org.usf.inspect.core.SessionManager;
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
public final class FilterExecutionMonitor extends OncePerRequestFilter implements HandlerInterceptor {

	static final String CURRENT_SESSION = FilterExecutionMonitor.class.getName() + ".session";
	static final String STAGE_START = FilterExecutionMonitor.class.getName() + ".stageStart";

	static final Collector<CharSequence, ?, String> joiner = joining("_");
	static final String TRACE_HEADER = "x-tracert"; //"x-inspect"

	private final Predicate<HttpServletRequest> excludeFilter;
	//v1.1
	private final HttpUserProvider userProvider;

	public FilterExecutionMonitor(HttpRouteMonitoringProperties config, HttpUserProvider userProvider) {
		Predicate<HttpServletRequest> filter = req-> false;
		if(!config.getExcludes().isEmpty()) {
			var pArr = config.excludedPaths();
			if(nonNull(pArr) && pArr.length > 0) {
				var matcher = new AntPathMatcher();
				filter = req-> stream(pArr).anyMatch(p-> matcher.match(p, req.getServletPath()));
			}
			var mArr = config.excludedMethods();
			if(nonNull(mArr) && mArr.length > 0) {
				filter = filter.or(req-> stream(mArr).anyMatch(m-> m.equals(req.getMethod())));
			}
		}
		this.excludeFilter = filter;
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
			context().reportEventHandleError("FilterExecutionMonitor.doFilterInternal", null, e);
			throw new IllegalStateException(e); 
		}
	}
	
	private void traceRestSession(HttpServletRequest req, HttpServletResponse res) {
		var start = now();
		var ses = (RestSession) req.getAttribute(CURRENT_SESSION);
		if(isNull(ses)) {
			ses = ofNullable(req.getHeader(TRACE_HEADER))
					.map(SessionManager::createRestSession)
					.orElseGet(SessionManager::createRestSession)
					.updateContext();
			try {
				ses.setStart(start);
				ses.setThreadName(threadName());
				ses.setMethod(req.getMethod());
				ses.setURI(fromRequest(req));
				ses.setAuthScheme(extractAuthScheme(req.getHeader(AUTHORIZATION))); //extract user !?
				ses.setInDataSize(req.getContentLength());
				ses.setInContentEncoding(req.getHeader(CONTENT_ENCODING));
				ses.setUserAgent(req.getHeader(USER_AGENT));
			}
			catch (Exception t) {
				context().reportEventHandleError("FilterExecutionMonitor.traceRestSession", ses, t);
			}
			finally {
				context().emitTrace(ses);
				res.addHeader(TRACE_HEADER, ses.getId()); //add headers before doFilter
				res.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
				req.setAttribute(CURRENT_SESSION, ses);
			}
		}
		else {
			ses.updateContext(); //deferred execution
		}
		req.setAttribute(STAGE_START, start); //SYNC & ASYNC stage start
	}
	
	private ExecutionMonitorListener<Void> restSessionListener(HttpServletRequest request, HttpServletResponse response) {
		return (s,e,o,t)-> {
			var ses = (RestSession) request.getAttribute(CURRENT_SESSION);
			if(!isAsyncStarted(request)) { //!Async || isAsyncDispatch
				ses.runSynchronized(()->{
					if(nonNull(response)) {
						ses.setStatus(response.getStatus());
						ses.setOutDataSize(response.getBufferSize()); //!exact size
						ses.setOutContentEncoding(response.getHeader(CONTENT_ENCODING)); 
						ses.setCacheControl(response.getHeader(CACHE_CONTROL));
						ses.setContentType(response.getContentType());
					}
					if(nonNull(t) && isNull(ses.getException())) { // see advise & interceptor
						ses.setException(fromException(t));
					}
					ses.setEnd(e);  //IO | CancellationException | ServletException => no ErrorHandler
				});
				return ses.releaseContext(); //emit session & clean context
			}
			else {
				context().emitTrace(ses.createStage(DEFERRED, s, e, t));
				if(nonNull(t)) {
					ses.runSynchronized(()-> ses.setEnd(e));
					return ses.releaseContext(); //emit session & clean context
				}
				return null; //do not trace this
			}
		};
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return excludeFilter.test(request);
	}

	/**
	 * Filter → Interceptor.preHandle → Controller → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		var ses = (RestSession) request.getAttribute(CURRENT_SESSION);
		if(nonNull(ses) && shouldIntercept(handler)) {  //avoid unfiltered request
			var now = now();
			try {
				var beg = (Instant) request.getAttribute(STAGE_START);
				context().emitTrace(ses.createStage(PRE_PROCESS, beg, now, null));
				request.setAttribute(STAGE_START, now);
			}
			catch (Exception t) {
				context().reportEventHandleError("FilterExecutionMonitor.preHandle", ses, t);
			}
		}
		return HandlerInterceptor.super.preHandle(request, response, handler);
	}
	
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		var ses = (RestSession) request.getAttribute(CURRENT_SESSION);
		if(nonNull(ses) && shouldIntercept(handler)) { //avoid unfiltered request
			var now = now();
			try {
				var beg = (Instant) request.getAttribute(STAGE_START);
				context().emitTrace(ses.createStage(PROCESS, beg, now, null));
				request.setAttribute(STAGE_START, now);
			}
			catch (Exception t) {
				context().reportEventHandleError("FilterExecutionMonitor.postHandle", ses, t);
			}
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		var ses = (RestSession) request.getAttribute(CURRENT_SESSION);
		if(nonNull(ses) && shouldIntercept(handler)) { //avoid unfiltered request 
			var now = now();
			try {
				var beg = (Instant) request.getAttribute(STAGE_START);
				context().emitTrace(ses.createStage(POST_PROCESS, beg, now, null));
				if(handler instanceof HandlerMethod mth) { //!static resource
					var name = resolveEndpointName(mth, request); 
					ses.runSynchronized(()->{
						ses.setName(name);
						ses.setUser(userProvider.getUser(request, name));
						if(nonNull(ex) && isNull(ses.getException())) {// unhandled exception in @ControllerAdvice
							ses.setException(fromException(ex));
						}
					});
				}
			}
			catch (Exception t) {
				context().reportEventHandleError("FilterExecutionMonitor.afterCompletion", ses, t);
			}
		}
	}
	
	private String resolveEndpointName(HandlerMethod mth, HttpServletRequest req) {
		if(nonNull(mth)) {
			var ant = mth.getMethodAnnotation(TraceableStage.class);
			if(nonNull(ant) && !ant.name().isEmpty()) {
				try {
					return evalExpression(ant.name(), 
							mth.getBean(), mth.getBeanType(), 
							new String[] {"request"}, new Object[] {req}).toString();
				}
				catch (Exception e) {
					context().reportEventHandleError(format("eval expression '%s' on %s.%s", 
							ant.name(), mth.getBeanType().getSimpleName(), mth.getMethod().getName()), currentSession(), e);
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
	
    static URI fromRequest(HttpServletRequest req) {
    	var c = req.getRequestURL().toString();
        return create(isNull(req.getQueryString()) ? c : c + '?' + req.getQueryString());
    }
    
    static boolean shouldIntercept(Object handler) {  //BasicErrorController 
		return handler instanceof HandlerMethod mth && 
				!(mth.getBean() instanceof ErrorController);
	}
}