package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.ofNullable;
import static org.usf.traceapi.core.TraceConfiguration.idProvider;
import static org.usf.traceapi.core.TraceConfiguration.localTrace;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class ApiTraceFilter implements Filter {

	static final String TRACE_HEADER = "tracert";
	
	private final ClientProvider clientProvider;
	private final TraceSender traceSender;
	
	private final String application;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    	var req = (HttpServletRequest) request;
    	var trc = new IncomingRequest(ofNullable(req.getHeader(TRACE_HEADER)).orElseGet(idProvider));
    	localTrace.set(trc);
    	var beg = currentTimeMillis();
    	try {
            chain.doFilter(req, response);
    	}
    	finally {
    		var fin = currentTimeMillis();
    		localTrace.remove();
    		trc.setUrl(req.getRequestURL().toString());
    		trc.setQuery(req.getQueryString());
    		trc.setMethod(req.getMethod());
    		trc.setContentType(response.getContentType());
			trc.setPrincipal(clientProvider.getClientId(req));
			trc.setApplication(application);
    		trc.setStart(beg);
    		trc.setEnd(fin);
    		if(response instanceof HttpServletResponse) {
    			trc.setStatus(((HttpServletResponse)response).getStatus());
    		}
    		try {
    			traceSender.send(trc);
    		}
    		catch(Exception e) {
				log.warn("error while tracing request : {}", request, e);
    		}
		}
	}
}