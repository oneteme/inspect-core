package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.traceapi.core.ApiTraceFilter.localTrace;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public final class ApiTraceInterceptor implements HandlerInterceptor { //AsyncHandlerInterceptor ?

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) throws Exception {
        var trace = localTrace.get();
        if(trace != null && handler instanceof HandlerMethod) {
            HandlerMethod m = (HandlerMethod) handler;
            TraceableApi a = m.getMethodAnnotation(TraceableApi.class);
            if(nonNull(a)) {
            	if(!a.endpoint().isEmpty()) {
            		trace.setEndpoint(lookup(req, a.endpoint(), false));
            	}
            	if(!a.resource().isEmpty()) {
            		trace.setResource(lookup(req, a.resource(), true));
            	}
            }
            if(isNull(trace.getEndpoint())) {
            	trace.setEndpoint(defaultEndpoint(m));
            }
            if(isNull(trace.getResource())) {
            	trace.setResource(defaultResource(req));
            }
        }
    }
    
    @SuppressWarnings("unchecked")
	private static String lookup(HttpServletRequest req, String key, boolean parameter) {
    	var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    	var res = map.get(key); // variable
    	if(parameter && isNull(res)) {
    		res = req.getParameter(key); // parameter 
    	}
    	return nonNull(res) ? res : key; // constant
    }

    @SuppressWarnings("unchecked")
    private static String defaultResource(HttpServletRequest req) {
    	var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    	return map.entrySet().stream().map(Entry::getValue).collect(joining("_")); //order ?
    }
    
    private static String defaultEndpoint(HandlerMethod m) {
    	var va = m.getMethodAnnotation(RequestMapping.class);
		return va == null ? null : Stream.of(va.value()[0].split("/"))
				.filter(v-> v.matches("^[\\w-]+$"))
				.collect(joining("_"));
    }
}
