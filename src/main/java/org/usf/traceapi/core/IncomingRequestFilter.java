package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.net.URI.create;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.traceapi.core.TraceConfiguration.idProvider;
import static org.usf.traceapi.core.TraceConfiguration.localTrace;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	
	private final String application;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    	var req = (HttpServletRequest) request;
    	var res = (HttpServletResponse) response;
    	var trc = new IncomingRequest(ofNullable(req.getHeader(TRACE_HEADER)).orElseGet(idProvider));
    	localTrace.set(trc);
    	var beg = currentTimeMillis();
    	try {
            chain.doFilter(req, response);
    	}
    	finally {
    		var fin = currentTimeMillis();
    		localTrace.remove();
    		var uri = create(req.getRequestURL().toString());
    		trc.setProtocol(uri.getScheme());
    		trc.setHost(uri.getHost());
    		trc.setPort(uri.getPort());
    		trc.setMethod(req.getMethod());
    		trc.setPath(req.getRequestURI()); // path
    		trc.setQuery(req.getQueryString());
    		trc.setContentType(res.getContentType());
			trc.setStatus(res.getStatus());
			trc.setSize(req.getContentLength());
			trc.setApplication(application);
    		trc.setStart(ofEpochMilli(beg));
    		trc.setEnd(ofEpochMilli(fin));
    		//customizable data see IncomingRequestInterceptor
    		if(isNull(trc.getClient())) {
    			trc.setClient(clientProvider.supply(req));
    		}
            if(isNull(trc.getEndpoint())) {
            	trc.setEndpoint(defaultEndpoint(req));
            }
            if(isNull(trc.getResource())) {
            	trc.setResource(defaultResource(req));
            }
    		try {
    			traceSender.send(trc);
    		}
    		catch(Exception e) {
				log.warn("error while tracing request : {}", request, e);
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