package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.traceapi.core.IncomingRequestFilter.joiner;

import java.util.Map;
import java.util.stream.Stream;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
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
        	var trace = (IncomingRequest) Helper.localTrace.get();
            if(nonNull(trace)) {
            	if(a.clientProvider() != ClientProvider.class){
            		trace.setClient(supplyClient(req, a.clientProvider()));
            	}
            	if(a.endpoint().length > 0) {
            		trace.setEndpoint(lookup(req, a.endpoint(), false));
            	}
            	if(a.resource().length > 0) {
            		trace.setResource(lookup(req, a.resource(), true));
            	}
            	if(!a.group().isEmpty()) {
            		trace.setGroup(a.group());
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
    
    private static String supplyClient(HttpServletRequest req, Class<? extends ClientProvider> clasz) { //simple impl.
		try {
			return clasz.getDeclaredConstructor().newInstance().supply(req);
		} catch (Exception e) {
			log.warn("cannot instantiate class " + clasz, e);
		}
		return null;
    }
    
}
