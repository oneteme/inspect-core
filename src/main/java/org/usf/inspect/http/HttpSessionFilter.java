package org.usf.inspect.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.usf.inspect.core.HttpUserProvider;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.TraceableStage;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.inspect.core.Helper.evalExpression;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.Monitor.assertMonitorNonNull;
import static org.usf.inspect.core.SpelEvaluator.evalMethodExpression;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

/**
 * Filters HTTP requests and coordinates session tracing with Spring MVC interceptors.
 */
@Slf4j
@RequiredArgsConstructor
public final class HttpSessionFilter extends OncePerRequestFilter implements HandlerInterceptor {

	static final String SESSION_MONITOR = "inspect-http-request-monitor";
	static final Collector<CharSequence, ?, String> joiner = joining("_");

	private final HttpRoutePredicate routePredicate;
	private final HttpUserProvider userProvider;

	/**
	 * Applies the tracing filter to the current HTTP request.
	 *
	 * @param req the current HTTP servlet request
	 * @param res the current HTTP servlet response
	 * @param filterChain the remaining filter chain
	 * @throws IOException if request processing fails with an I/O error
	 * @throws ServletException if request processing fails with a servlet error
	 */
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
			hub().reportError(false, "HttpSessionFilter.doFilterInternal", e);
			throw new IllegalStateException(e);
		}
	}

	private ExecutionListener<Void> filterHandler(HttpServletRequest req, HttpServletResponse res) {
		var mnt = currentHttpMonitor(req);
		if(isNull(mnt)) {
			mnt = new HttpSessionMonitor(req, res);
			req.setAttribute(SESSION_MONITOR, mnt);
		}
		return mnt.preFilter(()-> this.isAsyncStarted(req));
	}

	/**
	 * Determines whether the current request should skip tracing.
	 *
	 * @param request the current HTTP servlet request
	 * @return {@code true} when the request should not be filtered
	 * @throws ServletException if request evaluation fails
	 */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return !routePredicate.accept(request);
	}

	/**
	 * Indicates that asynchronous dispatches should continue to be filtered.
	 *
	 * @return {@code false} so async dispatches are traced
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() { //Callable | Differed | @Async
		return false;
	}

	/**
	 * Starts request pre-processing for handlers that participate in tracing.
	 *
	 * @param request the current HTTP servlet request
	 * @param response the current HTTP servlet response
	 * @param handler the selected handler
	 * @return {@code true} to continue request handling
	 * @throws Exception if interceptor processing fails
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(shouldIntercept(handler)) {  //avoid unfiltred request
			var mnt = currentHttpMonitor(request);
			if(assertMonitorNonNull(mnt, "HttpSessionFilter.preHandle")) {
				mnt.preProcess();
			}
		}
		return HandlerInterceptor.super.preHandle(request, response, handler);
	}

	/**
	 * Records the main processing stage after handler execution for traced requests.
	 *
	 * @param request the current HTTP servlet request
	 * @param response the current HTTP servlet response
	 * @param handler the selected handler
	 * @param modelAndView the model and view returned by the handler, if any
	 * @throws Exception if interceptor processing fails
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		if(shouldIntercept(handler)) { //avoid unfiltred request
			var mnt = currentHttpMonitor(request);
			if(assertMonitorNonNull(mnt, "HttpSessionFilter.postHandle")) {
				mnt.process();
			}
		}
	}

	/**
	 * Finalizes traced request processing after completion of the handler chain.
	 *
	 * @param request the current HTTP servlet request
	 * @param response the current HTTP servlet response
	 * @param handler the selected handler
	 * @param ex the exception raised during request processing, if any
	 * @throws Exception if interceptor processing fails
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		if(shouldIntercept(handler)) { //avoid unfiltred request 
			var mnt = currentHttpMonitor(request);
			if(assertMonitorNonNull(mnt, "HttpSessionFilter.afterCompletion")) {
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
				try {
					return evalExpression(ant.name(), 
							mth.getBean(), mth.getBeanType(), 
							new String[] {"request"}, new Object[] {req}).toString();
				}
				catch (Exception e) {
					log.warn("cannot eval expression ='{}' on {}.{}",
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