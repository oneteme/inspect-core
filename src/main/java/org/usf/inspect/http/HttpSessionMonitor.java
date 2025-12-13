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
import static org.usf.inspect.core.ExecutionMonitor.runSafely;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.HttpAction.DEFERRED;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.createHttpSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;

import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpSessionCallback;
import org.usf.inspect.core.Monitor;

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
public final class HttpSessionMonitor implements Monitor {
	
	private HttpSessionCallback call;
	private Instant lastTimestamp;
	private boolean async;
	
	public void preFilter(HttpServletRequest req, HttpServletResponse res){
		lastTimestamp = now();
		call = createHttpSession(lastTimestamp, req.getHeader(TRACE_HEADER), ses->{
			if(nonNull(req)) {
				ses.setMethod(req.getMethod());
				ses.setURI(fromRequest(req));
				ses.setAuthScheme(extractAuthScheme(req.getHeader(AUTHORIZATION))); //extract user !?
				ses.setDataSize(req.getContentLength());
				ses.setContentEncoding(req.getHeader(CONTENT_ENCODING));
				ses.setUserAgent(req.getHeader(USER_AGENT));
			}
			if(nonNull(res)) {
				res.addHeader(TRACE_HEADER, ses.getId()); //add headers before doFilter
				res.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
			}
		});
		setActiveContext(call);
	}
	
	public void deferredFilter(Instant end){
		if(assertStillOpened(call)) {
			runSafely(()-> emitStage(DEFERRED, end));
			clearContext(call);
		}
	}
	
	public void preProcess(Instant end){
		if(assertStillOpened(call)) {
			runSafely(()-> emitStage(PRE_PROCESS, end));
		}
	}
	
	public void process(Instant end){ //see this.asyncPostFilterHander
		if(assertStillOpened(call) && !async) {
			runSafely(()-> emitStage(PROCESS, end));
		}
	}

	public void postProcess(Instant end, String name, String user, Throwable thrw){
		if(assertStillOpened(call)) {
			runSafely(()-> {
				emitStage(POST_PROCESS, end);
				call.setName(name);
				call.setUser(user);
				if(nonNull(thrw) && isNull(call.getException())) {// unhandeled exception in @ControllerAdvice
					call.setException(fromException(thrw));
				}
			});
		}
	}
	
	public void handleError(Throwable thrw) {
		if(assertStillOpened(call) && isNull(call.getException())) {
			call.setException(fromException(thrw));
		}
	}
	
	public ExecutionHandler<Void> asyncPostFilterHander(Instant end, HttpServletResponse res){
		async = true;
		if(assertStillOpened(call)) {
			runSafely(()-> emitStage(PROCESS, end));
			setActiveContext(call);
		}
		return (s,e,o,t)-> postFilterHandler(e, res, t);
	}
	
	public void postFilterHandler(Instant end, HttpServletResponse response, Throwable thrw) {
		if(assertStillOpened(call)) {
			if(nonNull(response)) {
				call.setStatus(response.getStatus());
				call.setDataSize(response.getBufferSize()); //!exact size
				call.setContentEncoding(response.getHeader(CONTENT_ENCODING)); 
				call.setCacheControl(response.getHeader(CACHE_CONTROL));
				call.setContentType(response.getContentType());
			}
			if(nonNull(thrw) && isNull(call.getException())) { // see advise & interceptor
				call.setException(fromException(thrw));
			}
			call.setEnd(end);  //IO | CancellationException | ServletException => no ErrorHandler
			emit(call);
			clearContext(call);
			call = null;
		}
	}

	void emitStage(HttpAction action, Instant end) {
		var now = now();
		emit(call.createStage(action, lastTimestamp, end, null));
		lastTimestamp = now;
	}

    static URI fromRequest(HttpServletRequest req) {
    	var c = req.getRequestURL().toString();
        return create(isNull(req.getQueryString()) ? c : c + '?' + req.getQueryString());
    }
}