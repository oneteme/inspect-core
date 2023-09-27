package org.usf.traceapi.core;

import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.traceapi.core.ExceptionInfo.fromException;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.newInstance;
import static org.usf.traceapi.core.StageUpdater.getUser;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 
 * @author u$f
 *
 */
public final class IncomingRequestInterceptor implements HandlerInterceptor { //AsyncHandlerInterceptor ?

	static final Collector<CharSequence, ?, String> joiner = joining("_");
	
    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) throws Exception {
    	var in = (ApiSession) localTrace.get();
        if(nonNull(in)) {
			in.setName(defaultEndpointName(req));
        	in.setUser(getUser(req));
        	if(nonNull(ex) && isNull(in.getException())) {//already set with Aspect
        		in.setException(fromException(ex));
        	}
	        if(handler instanceof HandlerMethod) {//important! !static resource 
	        	HandlerMethod m = (HandlerMethod) handler;
	        	TraceableStage a = m.getMethodAnnotation(TraceableStage.class);
	            if(nonNull(a)) {
	            	if(!a.value().isBlank()) {
	        			in.setName(a.value());
	            	}
	            	if(a.sessionUpdater() != StageUpdater.class) {
	            		newInstance(a.sessionUpdater()).ifPresent(u-> u.update(in, req));
	            	}
	            	//no location
                }
            }
        }
    }

	@SuppressWarnings("unchecked")
	private static String defaultEndpointName(HttpServletRequest req) {
		var arr = req.getRequestURI().substring(1).split("/");
		var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		return map == null ? join("_", arr) : Stream.of(arr)
				.filter(not(map.values()::contains))
				.collect(joiner);
	}
}
