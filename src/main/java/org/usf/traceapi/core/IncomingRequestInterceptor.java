package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.traceapi.core.IncomingRequestFilter.joiner;
import static org.usf.traceapi.core.TraceConfiguration.localTrace;

import java.util.Map;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public final class IncomingRequestInterceptor implements HandlerInterceptor { //AsyncHandlerInterceptor ?

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return handler instanceof HandlerMethod; //important! !static resource 
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) throws Exception {
        HandlerMethod m = (HandlerMethod) handler;
        TraceableApi a = m.getMethodAnnotation(TraceableApi.class);
        if(nonNull(a)) {
        	var trace = localTrace.get();
            if(nonNull(trace)) {
            	if(a.endpoint().length > 0) {
            		trace.setEndpoint(lookup(req, a.endpoint(), false));
            	}
            	if(a.resource().length > 0) {
            		trace.setResource(lookup(req, a.resource(), true));
            	}
            }
        }
    }
    
    @SuppressWarnings("unchecked")
	private static String lookup(HttpServletRequest req, String[] keys, boolean parameter) {
    	var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    	return Stream.of(keys).map(key-> {
        	var res = map.get(key); // variable
        	if(parameter && isNull(res)) {
        		res = req.getParameter(key); // parameter 
        	}
        	return nonNull(res) ? res : key; // constant
    	}).collect(joiner);
    }
}
