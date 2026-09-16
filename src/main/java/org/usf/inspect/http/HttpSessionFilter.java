package org.usf.inspect.http;

import static jakarta.servlet.DispatcherType.ASYNC;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.inspect.core.DualEventTracer.assertActiveTracer;
import static org.usf.inspect.core.SpelEvaluator.evalMethodExpression;
import static org.usf.inspect.core.TraceDispatcherHub.hub;
import static org.usf.inspect.http.InspectServletRequestListener.SESSION_TRACER;
import static org.usf.inspect.http.WebUtils.TRACE_ID_HEADER;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.usf.inspect.core.HttpUserProvider;
import org.usf.inspect.core.TraceableStage;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
public final class HttpSessionFilter extends OncePerRequestFilter implements AsyncHandlerInterceptor {

	static final Collector<CharSequence, ?, String> joiner = joining("_");

	private final HttpRoutePredicate routePredicate;
	private final HttpUserProvider userProvider;
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
//		var cRes = new ContentCachingResponseWrapper(res) doesn't works with async
		var trc = requireActiveTracer(req, "HttpSessionFilter.doFilterInternal");
		if(isNull(trc)) {
			filterChain.doFilter(req, res);
		}
		else {
			var wrp = res;
			try {
				if(!res.containsHeader(TRACE_ID_HEADER)) {// avoid duplicate header in async dispatch
					var id = trc.getUpdate().getId();
					res.addHeader(TRACE_ID_HEADER, id.toString()); //add headers before doFilter
					res.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_ID_HEADER);
					wrp = new InspectResponseWrapper(res, trc.getStreamPayload());
				}
				if(req.getDispatcherType() == ASYNC) {
					trc.propagateContext();
					trc.emitExecutionStage();
				}
				filterChain.doFilter(req, wrp);
			}
			catch (ServletException e) {
				trc.handleError(nonNull(e.getCause()) ? e.getCause() : e); 
				throw e;
			}
			catch (Exception e) {
				trc.handleError(e);
				throw e;
			}
			finally {
				trc.setResponse(res);
			}
		}
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return !routePredicate.accept(request);
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() { //Callable | Differed | @Async
		return false;
	}
	
	@Override
	public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		var trc = requireActiveTracer(request, "HttpSessionFilter.afterConcurrentHandlingStarted");
        if (nonNull(trc)) {
            trc.emitDelegationStage(); //context will be propagated by task executor decorator
        }
	}
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(shouldIntercept(handler)) {  //avoid infiltrate request
			var trc = requireActiveTracer(request, "HttpSessionFilter.preHandle");
			if(nonNull(trc) && request.getDispatcherType() != ASYNC) {
				try {
					var name = resolveEndpointName(handler, request);
					var user = userProvider.getUser(request, name);
					trc.emitInitializationStage(name, user);
				}
				catch (Exception e) {
					hub().reportError("HttpSessionFilter.preHandle", e);
				}
			}
		}
		return AsyncHandlerInterceptor.super.preHandle(request, response, handler);
	}
	
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		if(shouldIntercept(handler)) { //avoid infiltrate request
			var trc = requireActiveTracer(request, "HttpSessionFilter.postHandle");
			if(nonNull(trc) && request.getDispatcherType() != ASYNC) {
				trc.emitExecutionStage();
			}
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		if(shouldIntercept(handler)) { //avoid infiltrate request 
			var trc = requireActiveTracer(request, "HttpSessionFilter.afterCompletion");
			if(nonNull(trc)) {
				if(nonNull(ex)) {
					trc.handleError(ex);
				}
				trc.emitFinalizationStage();
			}
		}
	}
	
	private String resolveEndpointName(Object handler, HttpServletRequest req) {
		if(handler instanceof HandlerMethod mth) {
			var ant = mth.getMethodAnnotation(TraceableStage.class);
			if(nonNull(ant) && !ant.name().isEmpty()) {
				return evalMethodExpression(ant.name(), mth.getBean(), mth.getMethod(), null);
//				return Helper.evalExpression(ant.name(), mth.getBean(), mth.getBeanType(), 
//						new String[] {"request"}, new Object[] {req}).toString()
			}
		}
		return defaultEndpointName(req);
	}
	
	@SuppressWarnings("unchecked")
	private static String defaultEndpointName(HttpServletRequest req) {
		var arr = req.getRequestURI().substring(1).split("/");
		var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		return isNull(map) ? join("_", arr) : Stream.of(arr)
				.filter(not(map.values()::contains))
				.collect(joiner);
	}

	static boolean shouldIntercept(Object handler) {  //BasicErrorController 
		return handler instanceof HandlerMethod mth && 
				!(mth.getBean() instanceof ErrorController);
	}
    
    static HttpSessionTracer currentHttpMonitor(HttpServletRequest req) {
    	return (HttpSessionTracer) req.getAttribute(SESSION_TRACER);
    }
    
    static HttpSessionTracer requireActiveTracer(HttpServletRequest req, String action) {
    	var c = currentHttpMonitor(req);
		return assertActiveTracer(c, action) ? c : null;
    }
	
	@SuppressWarnings("unchecked")
	static <X extends Throwable> void sneakyThrow(Throwable t) throws X {
	    throw (X) t;
	}
}