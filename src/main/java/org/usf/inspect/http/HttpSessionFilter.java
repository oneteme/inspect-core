package org.usf.inspect.http;

import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.inspect.core.DualEventTracer.assertActiveTracer;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.SpelEvaluator.evalMethodExpression;
import static org.usf.inspect.http.HttpSessionTracer.httpSessionTracer;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.usf.inspect.core.HttpUserProvider;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
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
public final class HttpSessionFilter extends OncePerRequestFilter implements HandlerInterceptor {

	static final String SESSION_TRACER = "inspect-http-session-tracer";
	static final Collector<CharSequence, ?, String> joiner = joining("_");

	private final HttpRoutePredicate routePredicate;
	private final HttpUserProvider userProvider;
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
//		var cRes = new ContentCachingResponseWrapper(res) doesn't works with async
		try {
			exec(()-> filterChain.doFilter(req, res), getTracer(req, res));	
		}
		catch (IOException | ServletException e) {
			throw e;
		}
		catch (Exception e) {
			sneakyThrow(e); //should never happen
		}
	}
	
	private ExecutionListener<Void> getTracer(HttpServletRequest req, HttpServletResponse res) {
		var mnt = currentHttpMonitor(req);
		if(isNull(mnt)) {
			mnt = httpSessionTracer(req, res, ()-> isAsyncStarted(req));
			req.setAttribute(SESSION_TRACER, mnt);
		}
		else {
			mnt.async();
		}
		return mnt;
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
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(shouldIntercept(handler)) {  //avoid infiltrate request
			var mnt = requireActiveTracer(request, "HttpSessionFilter.preHandle");
			if(nonNull(mnt)) {
				mnt.preProcess();
			}
		}
		return HandlerInterceptor.super.preHandle(request, response, handler);
	}
	
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		if(shouldIntercept(handler)) { //avoid infiltrate request
			var mnt = requireActiveTracer(request, "HttpSessionFilter.postHandle");
			if(nonNull(mnt)) {
				mnt.process();
			}
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		if(shouldIntercept(handler)) { //avoid infiltrate request 
			var mnt = requireActiveTracer(request, "HttpSessionFilter.afterCompletion");
			if(nonNull(mnt)) {
				var name = resolveEndpointName(handler, request);
				var user = userProvider.getUser(request, name);
				mnt.postProcess(name, user, ex);
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