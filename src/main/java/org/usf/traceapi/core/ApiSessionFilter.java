package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.net.URI.create;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.usf.traceapi.core.ApiSession.synchronizedApiSession;
import static org.usf.traceapi.core.ExceptionInfo.fromException;
import static org.usf.traceapi.core.Helper.applicationInfo;
import static org.usf.traceapi.core.Helper.extractAuthScheme;
import static org.usf.traceapi.core.Helper.idProvider;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.threadName;

import java.io.IOException;
import java.util.stream.Stream;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

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
public final class ApiSessionFilter extends OncePerRequestFilter {

	static final String TRACE_HEADER = "x-tracert";
	
	private final TraceSender traceSender;
	private final String[] excludeUrlPatterns;
	
	private final AntPathMatcher matcher = new AntPathMatcher();
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
    	var in = synchronizedApiSession(ofNullable(req.getHeader(TRACE_HEADER)).orElseGet(idProvider));
    	log.debug("incoming request : {} <= {}", in.getId(), req.getRequestURI());
    	localTrace.set(in);
		Throwable ex = null;
    	var beg = currentTimeMillis();
    	try {
    		filterChain.doFilter(req, res);
    	}
    	catch(Exception e) {
    		ex =  e;
    		throw e;
    	}
    	finally {
    		var fin = currentTimeMillis();
    		try {
	    		var uri = create(req.getRequestURL().toString());
	    		in.setMethod(req.getMethod());
	    		in.setProtocol(uri.getScheme());
	    		in.setHost(uri.getHost());
    			in.setPort(uri.getPort());
	    		in.setPath(req.getRequestURI());
	    		in.setQuery(req.getQueryString());
	    		in.setContentType(res.getContentType());
				in.setStatus(res.getStatus());
				in.setAuthScheme(extractAuthScheme(req.getHeader(AUTHORIZATION)));
				in.setInDataSize(req.getContentLength()); //not exact !?
				in.setOutDataSize(res.getBufferSize()); //not exact !?
	    		in.setStart(ofEpochMilli(beg));
	    		in.setEnd(ofEpochMilli(fin));
    			in.setThreadName(threadName());
    			in.setApplication(applicationInfo());
        		if(nonNull(ex) && isNull(in.getException())) { //already set in IncomingRequestInterceptor
        			in.setException(fromException(ex));
        		}
    			// name, user & exception delegated to interceptor
    			traceSender.send(in);
    		}
    		catch(Exception e) {
				log.warn("error while tracing : " + req, e);
				//do not throw exception
    		}
			localTrace.remove();
		}
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return nonNull(excludeUrlPatterns) && Stream.of(excludeUrlPatterns)
		        .anyMatch(p -> matcher.match(p, request.getServletPath()));
	}
	
}