package org.usf.inspect.http;

import static java.lang.String.join;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.Helper.evalExpression;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.Monitor.assertMonitorNonNull;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.HttpUserProvider;
import org.usf.inspect.core.TraceableStage;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f 
 *
 */
@Slf4j
public final class HttpSessionFilter extends OncePerRequestFilter implements HandlerInterceptor {

	static final String SESSION_MONITOR = "inspect-http-request-monitor";
	static final Collector<CharSequence, ?, String> joiner = joining("_");

	private final HttpRoutePredicate routePredicate;
	private final HttpUserProvider userProvider;

	public HttpSessionFilter(HttpRoutePredicate routePredicate, HttpUserProvider userProvider) {
		this.routePredicate = routePredicate;
		this.userProvider = userProvider;
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
//		var cRes = new ContentCachingResponseWrapper(res) doesn't works with async
		try {
			exec(()-> filterChain.doFilter(req, res), filterHandler(req, res));	
		}
		catch (IOException | ServletException | RuntimeException e) {
			throw e;
		}
		catch (Exception e) {//should never happen
			context().reportError(false, "HttpSessionFilter.doFilterInternal", e);
			throw new IllegalStateException(e); 
		}
	}
	
	private ExecutionListener<Void> filterHandler(HttpServletRequest req, HttpServletResponse res) {
		var prv = currentHttpMonitor(req);
		if(isNull(prv)) {
			var mnt = new HttpSessionMonitor();
			mnt.preFilter(req, res); //called once
			req.setAttribute(SESSION_MONITOR, mnt);
			return(s,e,o,t)-> {
				if(isAsyncStarted(req)) {
					mnt.deferredFilter(e); 
				}
				else {
					mnt.postFilterHandler(e, res, t);
				}
			};
		}
		else if(isAsyncDispatch(req))  {
			return prv.asyncPostFilterHander(now(), res);
		}
		return (s,e,o,t)-> context().reportMessage(false, 
				"HttpSessionFilter.filterHandler", "Unexpected HttpSessionMonitor in request attribute"); //do nothing
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
		var now = now();
		if(shouldIntercept(handler)) {  //avoid unfiltred request
			var mnt = currentHttpMonitor(request);
			if(assertMonitorNonNull(mnt, "HttpSessionFilter.preHandle")) {
				mnt.preProcess(now);
			}
		}
		return HandlerInterceptor.super.preHandle(request, response, handler);
	}
	
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		var now = now();
		if(shouldIntercept(handler) && !isAsyncDispatch(request)) { //avoid unfiltred request
			var mnt = currentHttpMonitor(request);
			if(assertMonitorNonNull(mnt, "HttpSessionFilter.postHandle")) {
				mnt.process(now);
			}
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		var now = now();
		if(shouldIntercept(handler)) { //avoid unfiltred request 
			var mnt = currentHttpMonitor(request);
			if(assertMonitorNonNull(mnt, "HttpSessionFilter.afterCompletion")) {
				var name = resolveEndpointName(handler, request);
				var user = userProvider.getUser(request, name);
				mnt.postProcess(now, name, user, ex);
			}
		}
	}
	
	private String resolveEndpointName(Object handler, HttpServletRequest req) {
		if(handler instanceof HandlerMethod mth) {
			var ant = mth.getMethodAnnotation(TraceableStage.class);
			if(nonNull(ant) && !ant.name().isEmpty()) {
				try {
					return evalExpression(ant.name(), 
							mth.getBean(), mth.getBeanType(), 
							new String[] {"request"}, new Object[] {req}).toString();
				}
				catch (Exception e) {
					log.warn("cannot eval expression ='%s' on %s.%s", 
							ant.name(), mth.getBeanType().getSimpleName(), mth.getMethod().getName());
				}
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
    
    static HttpSessionMonitor currentHttpMonitor(HttpServletRequest req) {
    	return (HttpSessionMonitor) req.getAttribute(SESSION_MONITOR);
    }
}