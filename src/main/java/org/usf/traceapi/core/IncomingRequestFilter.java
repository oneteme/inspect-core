package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.net.URI.create;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.traceapi.core.IncomingRequest.synchronizedIncomingRequest;
import static org.usf.traceapi.core.TraceConfiguration.idProvider;
import static org.usf.traceapi.core.TraceConfiguration.localTrace;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
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
	
	private final ClientProvider clientProvider;
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
				in.setInDataSize(req.getInputStream().available()); //not exact !?
				in.setOutDataSize(res.getBufferSize()); //not exact !?
	    		in.setStart(ofEpochMilli(beg));
	    		in.setEnd(ofEpochMilli(fin));
	    		in.setThread(currentThread().getName());
	    		//customizable data see IncomingRequestInterceptor
	    		if(isNull(in.getClient())) {
	    			in.setClient(clientProvider.supply(req));
	    		}
	            if(isNull(in.getEndpoint())) {
	            	in.setEndpoint(defaultEndpoint(req));
	            }
	            if(isNull(in.getResource())) {
	            	in.setResource(defaultResource(req));
	            }
	            in.setOs(getProperty("os.name"));
				in.setRe("java " + getProperty("java.version"));
	            //cannot override collection
    			traceSender.send(in);
    		}
    		catch(Exception e) {
				log.warn("error while tracing : {}", request, e);
    		}
		}
	}

    @SuppressWarnings("unchecked")
    private static String defaultEndpoint(HttpServletRequest req) {
    	var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		return map == null ? null : Stream.of(req.getRequestURI().split("/"))
				.filter(not(String::isEmpty))
				.filter(not(map.values()::contains))
				.collect(joiner);
    }

    @SuppressWarnings("unchecked")
    private static String defaultResource(HttpServletRequest req) {
    	var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    	return map == null ? null : map.entrySet().stream()
    			.map(Entry::getValue)
    			.collect(joiner); //order ?
    }
}