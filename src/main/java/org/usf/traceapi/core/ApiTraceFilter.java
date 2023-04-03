package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

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

	static final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
	static final ThreadLocal<MainRequest> localTrace = new InheritableThreadLocal<>();
	static final String TRACE_HEADER = "trace-api";
	
	private final ClientSupplier clientSupp;
	private final TraceSender sender;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    	var beg = currentTimeMillis();
    	var req = (HttpServletRequest) request;
    	var id  = ofNullable(req.getHeader(TRACE_HEADER)).orElseGet(randomUUID()::toString);
    	var mr  = new MainRequest(id, clientSupp.clientId(req), req.getRequestURL().toString(), req.getMethod(), beg);
    	localTrace.set(mr);
    	try {
            chain.doFilter(req, response);
    	}
    	finally {
    		mr.setEnd(currentTimeMillis());
			mr.setStatus(response == null ? null : ((HttpServletResponse)response).getStatus());
			executor.schedule(()-> sender.send(mr), 5, SECONDS); //wait 5s after sending results
		}
	}
}
