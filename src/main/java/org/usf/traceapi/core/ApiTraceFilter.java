package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ApiTraceFilter implements Filter {

	static final ThreadLocal<IncomingRequest> localTrace = new InheritableThreadLocal<>();
	static final String TRACE_HEADER = "request-uuid";
	
	private final ClientSupplier clientSupp;
	private final TraceSender sender;
	
	private final String application;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    	var beg = currentTimeMillis();
    	var req = (HttpServletRequest) request;
    	var id  = ofNullable(req.getHeader(TRACE_HEADER)).orElseGet(randomUUID()::toString);
    	var ic  = new IncomingRequest(id, 
    			req.getRequestURL().toString(), 
    			req.getMethod(), beg);
    	localTrace.set(ic);
    	try {
            chain.doFilter(req, response);
    	}
    	finally {
    		ic.setEnd(currentTimeMillis());
			localTrace.remove();
    		if(response != null) {
    			ic.setStatus(((HttpServletResponse)response).getStatus());
    			ic.setContentType(response.getContentType());
    		}
			//if not set
			ic.setPrincipal(clientSupp.clientId(req));
			ic.setApplication(application);
			sender.send(ic);
		}
	}
}
