package org.usf.inspect.rest;

import static java.lang.String.join;
import static java.net.URI.create;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.USER_AGENT;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.HttpAction.ASYNC;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.SessionManager.endSession;
import static org.usf.inspect.core.SessionManager.startRestSession;
import static org.usf.inspect.core.SessionManager.updateCurrentSession;
import static org.usf.inspect.core.TraceBroadcast.emit;
import static org.usf.inspect.rest.RestRequestInterceptor.httpRequestStage;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.HttpUserProvider;
import org.usf.inspect.core.RestSession;
import org.usf.inspect.core.RestSessionTrackConfiguration;
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
	
	private final HttpUserProvider userProvider;

	static final Collector<CharSequence, ?, String> joiner = joining("_");
	static final String TRACE_HEADER = "x-tracert";

	private final Predicate<HttpServletRequest> excludeFilter;

	public FilterExecutionMonitor(RestSessionTrackConfiguration config, HttpUserProvider userProvider) {
		Predicate<HttpServletRequest> pre = req-> false;
		if(!config.getExcludes().isEmpty()) {
			var pArr = config.excludedPaths();
			if(nonNull(pArr) && pArr.length > 0) {
				var matcher = new AntPathMatcher();
				pre = req-> Stream.of(pArr).anyMatch(p-> matcher.match(p, req.getServletPath()));
			}
			var mArr = config.excludedMethods();
			if(nonNull(mArr) && mArr.length > 0) {
				pre = pre.or(req-> Stream.of(mArr).anyMatch(m-> m.equals(req.getMethod())));
			}
		}
		this.excludeFilter = pre;
		this.userProvider = userProvider;
	}
	
	@Override
	protected boolean shouldNotFilterAsyncDispatch() { //Callable | Mono
		return false;
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
		var ses = traceRestSession(req, res);
		req.setAttribute(CURRENT_SESSION, ses);
		req.setAttribute(STAGE_START, now());
//		var cRes = new ContentCachingResponseWrapper(res) doesn't works with async
		try {
			exec(()-> filterChain.doFilter(req, res), restSessionListener(ses, req, res));	
		}
		catch (IOException | ServletException | RuntimeException e) {
			throw e;
		}
		catch (Exception e) {//should never happen
			throw new IllegalStateException(e); 
		}
		finally {
			if(!isAsyncStarted(req)) { //!Async || isAsyncDispatch
				req.removeAttribute(CURRENT_SESSION); //avoid intercepting BasicErrorController 
				req.removeAttribute(STAGE_START);
			}
			endSession(); //remove session from both (sync/async) thread local
		}
	}
	
	private RestSession traceRestSession(HttpServletRequest req, HttpServletResponse res) {
		var ses = (RestSession) req.getAttribute(CURRENT_SESSION);
		if(isNull(ses)) {
			var start = now();
			ses = startRestSession();
			try {
				res.addHeader(TRACE_HEADER, ses.getId()); //add headers before doFilter
				res.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
				ses.setStart(start);
				ses.setThreadName(threadName());
				ses.setMethod(req.getMethod());
				ses.setURI(fromRequest(req));
				ses.setAuthScheme(extractAuthScheme(req.getHeader(AUTHORIZATION))); //extract user !?
				ses.setInDataSize(req.getContentLength());
				ses.setInContentEncoding(req.getHeader(CONTENT_ENCODING));
				ses.setUserAgent(req.getHeader(USER_AGENT));
			} finally {
				emit(ses);
			}
		}
		else { // async
			updateCurrentSession(ses); //different thread
		}
		return ses;
	}
	
	private ExecutionMonitorListener<Void> restSessionListener(RestSession in, HttpServletRequest req, HttpServletResponse res) {
		return (s,e,o,t)-> {
			if(!isAsyncStarted(req)) { //!Async || isAsyncDispatch
				postProcess(req, e);
				var sttt = nonNull(res) ? res.getStatus() : 0;
				var size = nonNull(res) ? res.getBufferSize() : null; //!exact size
				var encd = nonNull(res) ? res.getHeader(CONTENT_ENCODING) : null; 
				var cach = nonNull(res) ? res.getHeader(CACHE_CONTROL) : null;
				var cntt = nonNull(res) ? res.getContentType() : null;
				in.lazy(()->{
					in.setStatus(sttt);
					in.setOutDataSize(size); //!exact size
					in.setOutContentEncoding(encd); 
					in.setCacheControl(cach);
					in.setContentType(cntt);
					in.setEnd(e);
					emit(in);
				});
			}
			else { //IO | CancellationException | ServletException => no ErrorHandler
				emit(httpRequestStage(in.getRest(), ASYNC, s, e, t));
				if(nonNull(t)) {
					in.lazy(()-> {
						in.setEnd(e);
						emit(in);
					});
				}
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
		var now = now();
		var ses = (RestSession) request.getAttribute(CURRENT_SESSION);
		if(nonNull(ses)) {
			var beg = (Instant) request.getAttribute(STAGE_START);
			emit(httpRequestStage(ses.getRest(), PRE_PROCESS, beg, now, null));
			request.setAttribute(STAGE_START, now);
		}
		return HandlerInterceptor.super.preHandle(request, response, handler);
	}
	
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		var now = now();
		var ses = (RestSession) request.getAttribute(CURRENT_SESSION);
		if(nonNull(ses)) {
			var beg = (Instant) request.getAttribute(STAGE_START);
			emit(httpRequestStage(ses.getRest(), PROCESS, beg, now, null));
			request.setAttribute(STAGE_START, now);
		}
	}

	public void postProcess(HttpServletRequest request, Instant end) {
		var ses = (RestSession) request.getAttribute(CURRENT_SESSION);
		if(nonNull(ses)) {
			var beg = (Instant) request.getAttribute(STAGE_START);
			emit(httpRequestStage(ses.getRest(), POST_PROCESS, beg, end, null));
		}
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		var ses = (RestSession) request.getAttribute(CURRENT_SESSION);
		if(nonNull(ses) && handler instanceof HandlerMethod mth) {
			String name = resolveEndpointName(mth, request); 
			ses.setName(name);
			ses.setUser(userProvider.getUser(request, name));
			if(nonNull(ex)) {// unhandled exception in @ControllerAdvice
				ses.setException(mainCauseException(ex));
			}
		}
	}

	private String resolveEndpointName(HandlerMethod mth, HttpServletRequest req) {
		if(nonNull(mth)) {
			var ant = mth.getMethodAnnotation(TraceableStage.class);
			if(nonNull(ant)) {
				return ant.value().isEmpty() ? null : ant.value(); 
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
}