package org.usf.inspect.web.mvc;

import static jakarta.servlet.DispatcherType.ASYNC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.inspect.Helper.formatLocation;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.SpelEvaluator.evalMethodTemplate;
import static org.usf.inspect.web.WebUtils.TRACE_ID_HEADER;
import static org.usf.inspect.web.mvc.InspectServletRequestListener.isSessionIgnored;
import static org.usf.inspect.web.mvc.InspectServletRequestListener.requireSessionTracer;

import java.io.IOException;
import java.util.Map;

import org.springframework.boot.webmvc.error.ErrorController;
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

/**
 * 
 * @author u$f 
 *
 */
@RequiredArgsConstructor
public final class HttpSessionFilter extends OncePerRequestFilter implements AsyncHandlerInterceptor {

	private final HttpUserProvider userProvider;
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
		var trc = requireSessionTracer(req, "doFilterInternal"); //created in InspectServletRequestListener
		if(isNull(trc)) {
			filterChain.doFilter(req, res);
		}
		else {
			try {
				if(!res.containsHeader(TRACE_ID_HEADER)) {// avoid duplicate header in async dispatch
					var id = trc.getUpdate().getId();
					res.addHeader(TRACE_ID_HEADER, id.toString()); //add headers before doFilter
					res.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_ID_HEADER);
				}
				if(req.getDispatcherType() == ASYNC) {
					trc.propagateContext(); //different thread
					trc.emitExecutionStage();
				}
				var wrp = new InspectHttpServletResponseWrapper(res, trc);
				trc.setResponse(wrp); //different async. response
				filterChain.doFilter(req, wrp);
			}
			catch (ServletException e) { //wrapped functional exception 
				trc.emitError(nonNull(e.getCause()) ? e.getCause() : e); 
				throw e;
			}
			catch (Exception e) {
				trc.emitError(e);
				throw e;
			}
		}
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return isSessionIgnored(request);
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false; //Callable | Differed | @Async
	}
	
	@Override
	public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(shouldIntercept(handler)) {  //avoid infiltrate request
			var trc = requireSessionTracer(request, "afterConcurrentHandlingStarted");
			if (nonNull(trc)) {
				trc.emitDelegationStage(); //context will be propagated by task executor decorator
			}
		}
	}
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(shouldIntercept(handler) && request.getDispatcherType() != ASYNC) {  //avoid infiltrate request
			var trc = requireSessionTracer(request, "preHandle");
			if(nonNull(trc)) {
				String name = null;
				String user = null;
				String lct = null;
				try {
					name = resolveEndpointName(handler, request);
					user = userProvider.getUser(request, name); //move into signal
					var hm = (HandlerMethod) handler;
					lct = formatLocation(hm.getBeanType().getName(), hm.getMethod().getName());
				}
				catch (Exception e) {
					REPORTER.error("HttpSessionFilter.preHandle: failed to resolve endpoint name or user", e);
				}
				finally {
					trc.emitInitializationStage(name, user, lct);
				}
			}
		}
		return AsyncHandlerInterceptor.super.preHandle(request, response, handler);
	}
	
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		if(shouldIntercept(handler) && request.getDispatcherType() != ASYNC) { //avoid infiltrate request
			var trc = requireSessionTracer(request, "postHandle");
			if(nonNull(trc)) {
				trc.emitExecutionStage();
			}
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		if(shouldIntercept(handler)) { //avoid infiltrate request 
			var trc = requireSessionTracer(request, "afterCompletion");
			if(nonNull(trc)) {
				trc.emitFinalizationStage();
				if(nonNull(ex)) {
					trc.emitError(ex);
				}
			}
		}
	}
	
	private String resolveEndpointName(Object handler, HttpServletRequest req) {
		if(handler instanceof HandlerMethod mth) {
			var ant = mth.getMethodAnnotation(TraceableStage.class);
			if(nonNull(ant) && !ant.name().isEmpty()) {
				return evalMethodTemplate(ant.name(), mth.getBean(), mth.getMethod(), null);
//				return Helper.evalExpression(ant.name(), mth.getBean(), mth.getBeanType(), 
//						new String[] {"request"}, new Object[] {req}).toString()
			}
		}
		return defaultEndpointName(req);
	}
	
	private static String defaultEndpointName(HttpServletRequest req) {
		var attr = req.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE);
		if(attr instanceof String str && !str.isEmpty()) {
			if(req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map vars && !vars.isEmpty()) {
				str = str.replaceAll("\\{(\\w+)\\}", "\\$$1");
			}
			return str.substring(1).replace("/", "_");
		}
		return null;
	}
	
	static boolean shouldIntercept(Object handler) {  //BasicErrorController 
		return handler instanceof HandlerMethod mth && 
				!(mth.getBean() instanceof ErrorController);
	}
}