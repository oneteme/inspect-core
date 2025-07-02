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
import static org.usf.inspect.core.HttpAction.EXEC;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;
import static org.usf.inspect.core.SessionManager.startRestSession;
import static org.usf.inspect.core.SessionManager.updateCurrentSession;
import static org.usf.inspect.core.SessionPublisher.emit;
import static org.usf.inspect.rest.RestRequestInterceptor.httpRequestStage;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
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
public final class RestSessionFilter extends OncePerRequestFilter implements HandlerInterceptor {

	static final String ASYNC_SESSION = RestSessionFilter.class.getName() + ".asyncSession";
	
	private final HttpUserProvider userProvider;

	static final Collector<CharSequence, ?, String> joiner = joining("_");
	static final String TRACE_HEADER = "x-tracert";

	private final Predicate<HttpServletRequest> excludeFilter;

	public RestSessionFilter(RestSessionTrackConfiguration config, HttpUserProvider userProvider) {
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
	}
	
	private RestSession traceRestSession(HttpServletRequest req, HttpServletResponse res) {
		var ses = (RestSession) req.getAttribute(ASYNC_SESSION);
		if(isNull(ses)) {
			ses = startRestSession();
			try {
				res.addHeader(TRACE_HEADER, ses.getId()); //add headers before doFilter
				res.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
				ses.setStart(now());
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
		if(!isAsyncStarted(req)) { //!Async || isAsyncDispatch
			return (s,e,o,t)-> {
				var sttt = nonNull(res) ? res.getStatus() : 0;
				var size = nonNull(res) ? res.getBufferSize() : null; //!exact size
				var encd = nonNull(res) ? res.getHeader(CONTENT_ENCODING) : null; 
				var cach = nonNull(res) ? res.getHeader(CACHE_CONTROL) : null;
				var cntt = nonNull(res) ? res.getContentType() : null;
				emit(httpRequestStage(in.getRest(), EXEC, s, e, t));
				in.lazy(()->{
					in.setStatus(sttt);
					in.setOutDataSize(size); //!exact size
					in.setOutContentEncoding(encd); 
					in.setCacheControl(cach);
					in.setContentType(cntt);
					in.setEnd(e);
				});
			};
		}
		return (s,e,o,t)-> { // Async && isAsyncStarted
			if(nonNull(t)) { //IO | CancellationException | ServletException => no ErrorHandler
				req.setAttribute(ASYNC_SESSION, in);
				emit(httpRequestStage(in.getRest(), ASYNC, s, e, t));
				in.lazy(()-> in.setEnd(e));
			}
		};
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return excludeFilter.test(request);
	}
	
	@Override //append stage !?
	public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) throws Exception {
		var mth = (handler instanceof HandlerMethod o) ? o : null;
 		if(!shouldNotFilter(req) && (isNull(mth) || BasicErrorController.class != mth.getBean().getClass())) { //exclude spring controller, called twice : after throwing exception
			var ses = requireCurrentSession(RestSession.class); 
			if(nonNull(ses)) {
				String name = nonNull(mth) ? getNameFromAnnotation(mth) : null;
				if(isNull(name)) {
					name = defaultEndpointName(req);
				}
				ses.setName(name);
				ses.setUser(userProvider.getUser(req, name));
				if(nonNull(ex)) {//may be already set in Controller Advise
					ses.appendException(mainCauseException(ex));
				}
			}
		}
	}
	
	private String getNameFromAnnotation(HandlerMethod mth) {
		var ant = mth.getMethodAnnotation(TraceableStage.class);
		if(nonNull(ant)) {
			return ant.value().isEmpty() ? null : ant.value(); 
		}
		return null;
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