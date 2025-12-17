package org.usf.inspect.http;

import static java.net.URI.create;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.USER_AGENT;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.InspectExecutor.runSafely;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.HttpAction.DEFERRED;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.Monitor.assertStillOpened;
import static org.usf.inspect.core.Monitor.traceAround;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.createHttpSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpSession2;
import org.usf.inspect.core.HttpSessionCallback;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 
 * Filter → Interceptor.preHandle → Deferred → Controller(task-?) →   Filter → Interceptor.preHandle → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
 * Filter → Interceptor.preHandle → Controller → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
 * 
 * @author u$f 
 *
 */
@RequiredArgsConstructor
public final class HttpSessionMonitor  {
	
	private ExecutionListener<HttpServletResponse> handler;
	
	private Instant lastTimestamp;
	private HttpSessionCallback callback;
	private boolean async;
	
	public void preFilter(HttpServletRequest request, HttpServletResponse response){
		lastTimestamp = now();
		handler = traceAround(createHttpSession(lastTimestamp, request.getHeader(TRACE_HEADER)), this::createCallback,
				ses->{
					if(nonNull(request)) {
						ses.setMethod(request.getMethod());
						ses.setURI(fromRequest(request));
						ses.setAuthScheme(extractAuthScheme(request.getHeader(AUTHORIZATION))); //extract user !?
						ses.setDataSize(request.getContentLength());
						ses.setContentEncoding(request.getHeader(CONTENT_ENCODING));
						ses.setUserAgent(request.getHeader(USER_AGENT));
					}
					if(nonNull(response)) {
						response.addHeader(TRACE_HEADER, ses.getId()); //add headers before doFilter
						response.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
					}
				}, 
				(call, res)->{
					if(nonNull(res)) {
						call.setStatus(res.getStatus());
						call.setDataSize(res.getBufferSize()); //!exact size
						call.setContentEncoding(res.getHeader(CONTENT_ENCODING)); 
						call.setCacheControl(res.getHeader(CACHE_CONTROL));
						call.setContentType(res.getContentType());
					}
				});
	}
	
	//callback should be created before processing
	HttpSessionCallback createCallback(HttpSession2 session) { 
		return callback = session.createCallback();
	}
	
	public void deferredFilter(Instant end){
		if(assertStillOpened(callback, "HttpSessionMonitor.deferredFilter")) {
			runSafely(()-> emitStage(DEFERRED, end));
			clearContext(callback);
		}
	}
	
	public void preProcess(Instant end){
		if(assertStillOpened(callback, "HttpSessionMonitor.preProcess")) {
			runSafely(()-> emitStage(PRE_PROCESS, end));
		}
	}
	
	public void process(Instant end){ //see this.asyncPostFilterHander
		if(!async && assertStillOpened(callback, "HttpSessionMonitor.process")) {
			runSafely(()-> emitStage(PROCESS, end));
		}
	}

	public void postProcess(Instant end, String name, String user, Throwable thrw){
		if(assertStillOpened(callback, "HttpSessionMonitor.postProcess")) {
			runSafely(()-> {
				emitStage(POST_PROCESS, end);
				callback.setName(name);
				callback.setUser(user);
				if(nonNull(thrw) && isNull(callback.getException())) {// unhandeled exception in @ControllerAdvice
					callback.setException(fromException(thrw));
				}
			});
		}
	}
	
	public void handleError(Throwable thrw) {
		if(assertStillOpened(callback, "HttpSessionMonitor.handleError") && isNull(callback.getException())) {
			callback.setException(fromException(thrw));
		}
	}
	
	public ExecutionListener<Void> asyncPostFilterHander(Instant end, HttpServletResponse res){
		async = true;
		if(assertStillOpened(callback, "HttpSessionMonitor.asyncPostFilterHander")) {
			runSafely(()-> emitStage(PROCESS, end));
			setActiveContext(callback);
		}
		return (s,e,o,t)-> postFilterHandler(e, res, t);
	}
	
	public void postFilterHandler(Instant end, HttpServletResponse response, Throwable thrw) {
		if(nonNull(handler) && assertStillOpened(callback, "HttpSessionMonitor.postFilterHandler")) {
			handler.fire(null, end, response, thrw);
		}
	}

	void emitStage(HttpAction action, Instant end) {
		var now = now();
		context().emitTrace(callback.createStage(action, lastTimestamp, end, null));
		lastTimestamp = now;
	}

    static URI fromRequest(HttpServletRequest req) {
    	var c = req.getRequestURL().toString();
        return create(isNull(req.getQueryString()) ? c : c + '?' + req.getQueryString());
    }
}