package org.usf.traceapi.core;

import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.net.URI.create;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.traceapi.core.Helper.applicationInfo;
import static org.usf.traceapi.core.Helper.defaultUserProvider;
import static org.usf.traceapi.core.Helper.extractAuthScheme;
import static org.usf.traceapi.core.Helper.idProvider;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.IncomingRequest.synchronizedIncomingRequest;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class IncomingRequestFilter implements Filter {

	static final Collector<CharSequence, ?, String> joiner = joining("_");
	static final String TRACE_HEADER = "tracert";
	
	private final TraceSender traceSender;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    	var req = (HttpServletRequest) request;
    	var res = (HttpServletResponse) response;
    	var in  = synchronizedIncomingRequest(ofNullable(req.getHeader(TRACE_HEADER)).orElseGet(idProvider));
    	localTrace.set(in);
    	var beg = currentTimeMillis();
    	try {
            chain.doFilter(request, response);
    	}
    	finally {
    		var fin = currentTimeMillis();
    		try {
	    		localTrace.remove();
	    		var uri = create(req.getRequestURL().toString());
	    		in.setProtocol(uri.getScheme());
	    		in.setHost(uri.getHost());
	    		in.setPort(uri.getPort());
	    		in.setMethod(req.getMethod());
	    		in.setPath(req.getRequestURI()); // path
	    		in.setQuery(req.getQueryString());
	    		in.setContentType(res.getContentType());
				in.setStatus(res.getStatus());
				in.setAuthScheme(extractAuthScheme(req.getHeader(AUTHORIZATION)));
				in.setInDataSize(req.getInputStream().available()); //not exact !?
				in.setOutDataSize(res.getBufferSize()); //not exact !?
	    		in.setStart(ofEpochMilli(beg));
	    		in.setEnd(ofEpochMilli(fin));
	    		if(isNull(in.getName())) {//already set in IncomingRequestInterceptor
	    			in.setName(defaultEndpointName(req));
	    		}
	    		if(isNull(in.getUser())) {//already set in IncomingRequestInterceptor
	    			in.setUser(defaultUserProvider().getUser(req));
	    		}
	    		in.setThreadName(threadName());
	    		in.setApplication(applicationInfo());
    			traceSender.send(in);
    		}
    		catch(Exception e) {
				//do not catch exception
				log.warn("error while tracing : {}", request, e);
    		}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static String defaultEndpointName(HttpServletRequest req) {
		var arr = req.getRequestURI().split("/");
		var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		return map == null ? join("_", arr) : Stream.of(arr)
				.filter(not(String::isEmpty))
				.filter(not(map.values()::contains))
				.collect(joiner);
	}
	
}