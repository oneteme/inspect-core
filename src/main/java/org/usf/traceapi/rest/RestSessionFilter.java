package org.usf.traceapi.rest;

import static java.lang.String.join;
import static java.net.URI.create;
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
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.extractAuthScheme;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.newInstance;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.RestSession.synchronizedApiSession;
import static org.usf.traceapi.core.StageTracker.exec;
import static org.usf.traceapi.core.StageUpdater.getUser;
import static org.usf.traceapi.core.TraceMultiCaster.emit;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.usf.traceapi.core.RestSession;
import org.usf.traceapi.core.StageUpdater;
import org.usf.traceapi.core.TraceableStage;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f 
 *
 */
@RequiredArgsConstructor
public final class RestSessionFilter extends OncePerRequestFilter implements HandlerInterceptor {
	
	static final Collector<CharSequence, ?, String> joiner = joining("_");

	static final String TRACE_HEADER = "x-tracert";
	
	private final String[] excludeUrlPatterns;
	
	private final AntPathMatcher matcher = new AntPathMatcher();
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
    	var in = synchronizedApiSession();
    	localTrace.set(in);
		res.addHeader(TRACE_HEADER, in.getId());
		res.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
		var cRes = new ContentCachingResponseWrapper(res);
    	try {
        	exec(()-> filterChain.doFilter(req, cRes), (s,e,o,t)->{
	    		var uri = create(req.getRequestURL().toString());
	    		in.setMethod(req.getMethod());
	    		in.setProtocol(uri.getScheme());
	    		in.setHost(uri.getHost());
    			in.setPort(uri.getPort());
	    		in.setPath(req.getRequestURI());
	    		in.setQuery(req.getQueryString());
	    		in.setContentType(res.getContentType());
				in.setStatus(res.getStatus());
				in.setAuthScheme(extractAuthScheme(req.getHeader(AUTHORIZATION))); //extract user !?
				in.setInDataSize(req.getContentLength());
				in.setOutDataSize(cRes.getContentSize()); //exact size
				in.setInContentEncoding(req.getHeader(CONTENT_ENCODING));
				in.setOutContentEncoding(res.getHeader(CONTENT_ENCODING)); 
				in.setCacheControl(res.getHeader(CACHE_CONTROL));
				in.setUserAgent(req.getHeader(USER_AGENT));
	    		in.setStart(s);
	    		in.setEnd(e);
    			in.setThreadName(threadName());
        		if(nonNull(t) && isNull(in.getException())) { //may be set in TraceableAspect::aroundAdvice
        			in.setException(mainCauseException(t));
        		}
    			// name, user & exception delegated to intercepter
        		emit(in);
        	});	
    	}
    	catch (IOException | ServletException | RuntimeException e) {
    		throw e;
    	}
    	catch (Exception e) { //should never happen
    		throw new IllegalStateException(e); 
    	}
    	finally {
			localTrace.remove();
		}
		cRes.copyBodyToResponse();
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return nonNull(excludeUrlPatterns) && Stream.of(excludeUrlPatterns)
		        .anyMatch(p -> matcher.match(p, request.getServletPath()));
	}
	
    @Override //Session stage !?
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) throws Exception {
    	var in = (RestSession) localTrace.get();
        if(nonNull(in)) {
			in.setName(defaultEndpointName(req));
        	in.setUser(getUser(req));
        	if(nonNull(ex) && isNull(in.getException())) {//may be already set in Controller Advise
        		in.setException(mainCauseException(ex));
        	}
	        if(handler instanceof HandlerMethod hm) {//important! !static resource 
	        	TraceableStage a = hm.getMethodAnnotation(TraceableStage.class);
	            if(nonNull(a)) {
	            	if(!a.value().isBlank()) {
	        			in.setName(a.value());
	            	}
	            	if(a.sessionUpdater() != StageUpdater.class) {
	            		newInstance(a.sessionUpdater())
	            		.ifPresent(u-> u.update(in, req));
	            	}
                }
            }
        }
        //else !?
    }

	@SuppressWarnings("unchecked")
	private static String defaultEndpointName(HttpServletRequest req) {
		var arr = req.getRequestURI().substring(1).split("/");
		var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		return map == null ? join("_", arr) : Stream.of(arr)
				.filter(not(map.values()::contains))
				.collect(joiner);
	}
	
}