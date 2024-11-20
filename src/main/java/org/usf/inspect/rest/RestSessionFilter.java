package org.usf.inspect.rest;

import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static java.lang.String.join;
import static java.net.URI.create;
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
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.Helper.newInstance;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.endSession;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;
import static org.usf.inspect.core.SessionManager.updateCurrentSession;
import static org.usf.inspect.core.SessionPublisher.emit;
import static org.usf.inspect.core.StageTracker.exec;
import static org.usf.inspect.core.StageUpdater.getUser;

import java.io.IOException;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.usf.inspect.core.RestSession;
import org.usf.inspect.core.RestSessionTrackConfiguration;
import org.usf.inspect.core.SessionManager;
import org.usf.inspect.core.StageUpdater;
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

	static final Collector<CharSequence, ?, String> joiner = joining("_");
	static final String TRACE_HEADER = "x-tracert";

	private final Predicate<HttpServletRequest> excludeFilter;

	public RestSessionFilter(RestSessionTrackConfiguration config) {
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
	}
	
	@Override
	protected boolean shouldNotFilterAsyncDispatch() { //Callable | Mono
		return false;
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
		var in = ofNullable(req.getAttribute(ASYNC_SESSION))
				.map(o->{
					var rs = (RestSession) o;
					updateCurrentSession(rs); //different thread
					return rs;
				})
				.orElseGet(SessionManager::startRestSession);
//		var cRes = new ContentCachingResponseWrapper(res) doesn't works with async
		try {
			exec(()-> filterChain.doFilter(req, res), (s,e,o,t)-> {
				if(!isAsyncDispatch(req)) { // asyncStarted || sync
					in.setStart(s);
					in.setThreadName(threadName());
					var uri = create(req.getRequestURL().toString());
					in.setProtocol(uri.getScheme());
					in.setHost(uri.getHost());
					in.setPort(uri.getPort());
					in.setMethod(req.getMethod());
					in.setPath(req.getRequestURI());
					in.setQuery(req.getQueryString());
					in.setAuthScheme(extractAuthScheme(req.getHeader(AUTHORIZATION))); //extract user !?
					in.setInDataSize(req.getContentLength());
					in.setInContentEncoding(req.getHeader(CONTENT_ENCODING));
					in.setUserAgent(req.getHeader(USER_AGENT));
					res.addHeader(TRACE_HEADER, in.getId());
					res.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
				}
				if(!isAsyncStarted(req)) { // asyncDispatch || sync
					in.setEnd(e);
					in.setStatus(res.getStatus());
					in.setOutDataSize(res.getBufferSize()); //!exact size
					in.setOutContentEncoding(res.getHeader(CONTENT_ENCODING)); 
					in.setCacheControl(res.getHeader(CACHE_CONTROL));
					in.setContentType(res.getContentType());
				}
				else { //asyncStarted
					req.setAttribute(ASYNC_SESSION, in);
				}
				if(nonNull(t)) { //IO | no ErrorHandler
					in.setStatus(SC_INTERNAL_SERVER_ERROR); // overwrite default response status
					in.setException(mainCauseException(t));
				}
			});	
		}
		catch (IOException | ServletException | RuntimeException e) {
			throw e;
		}
		catch (Exception e) { //should never happen
			throw new IllegalStateException(e); 
		}
		finally {
			if(!isAsyncStarted(req)) {
				endSession();
				emit(in);
			}
		}
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return excludeFilter.test(request);
	}
	
	@Override //Session stage !?
	public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) throws Exception {
		var hm = (handler instanceof HandlerMethod o) ? o : null;
 		if(!shouldNotFilter(req) && (isNull(hm) || BasicErrorController.class != hm.getBean().getClass())) { //exclude spring controller, called twice : after throwing exception
			var ses = requireCurrentSession(RestSession.class); 
			if(nonNull(ses)) {
				ses.setName(defaultEndpointName(req));
				ses.setUser(getUser(req));
				if(nonNull(ex) && isNull(ses.getException())) {//may be already set in Controller Advise
					ses.setException(mainCauseException(ex));
				}
				if(nonNull(hm)) {//important! !static resource 
					TraceableStage a = hm.getMethodAnnotation(TraceableStage.class);
					if(nonNull(a)) {
						if(!a.value().isBlank()) {
							ses.setName(a.value());
						}
						if(a.sessionUpdater() != StageUpdater.class) {
							newInstance(a.sessionUpdater())
							.ifPresent(u-> u.update(ses, req));
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static String defaultEndpointName(HttpServletRequest req) {
		var arr = req.getRequestURI().substring(1).split("/");
		var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		return isNull(map) ? join("_", arr) : Stream.of(arr)
				.filter(not(map.values()::contains))
				.collect(joiner);
	}
}