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
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.HttpAction.DEFERRED;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.Monitor.assertStillOpened;
import static org.usf.inspect.core.Monitor.traceAtomic;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.createHttpSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;
import java.util.function.BooleanSupplier;

import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpSession2;
import org.usf.inspect.core.HttpSessionCallback;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

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
public final class HttpSessionMonitor {
	
	private final ExecutionListener<Void> handler;
	
	private Instant lastTimestamp;
	private HttpSessionCallback callback;
	private boolean async;
	
	public HttpSessionMonitor(HttpServletRequest request, HttpServletResponse response) {
		this.lastTimestamp = now();
		this.handler = traceAtomic(createHttpSession(lastTimestamp, request.getHeader(TRACE_HEADER)), this::createCallback,
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
					if(nonNull(response)) {
						call.setStatus(response.getStatus());
						call.setDataSize(response.getBufferSize()); //!exact size
						call.setContentEncoding(response.getHeader(CONTENT_ENCODING)); 
						call.setCacheControl(response.getHeader(CACHE_CONTROL));
						call.setContentType(response.getContentType());
					}
				});
	}
	
	//callback should be created before processing
	HttpSessionCallback createCallback(HttpSession2 session) { 
		return callback = session.createCallback();
	}
	
	public ExecutionListener<Void> preFilter(BooleanSupplier isAsync) {
		if(async && assertStillOpened(callback, "HttpSessionMonitor.preFilter")) { //!important async can be true or not set yet
			emitStage(PROCESS);
			setActiveContext(callback); //new Thread
		}
		return (s,e,o,t)-> {
			if(async = isAsync.getAsBoolean()) {
				emitStage(DEFERRED);
				if(nonNull(callback)) {
					clearContext(callback);
				}
			}
			else {
				handler.safeHandle(s, e, o, t);
			}
		};
	}
	
	public void preProcess(){
		emitStage(PRE_PROCESS);
	}
	
	public void process(){ //see this.asyncPostFilterHander
		if(!async) {
			emitStage(PROCESS);
		}
	}

	public void postProcess(String name, String user, Throwable thrw){
		if(assertStillOpened(callback, "HttpSessionMonitor.postProcess")) {
			emitStage(POST_PROCESS);
			try{
				callback.setName(name);
				callback.setUser(user);
				if(nonNull(thrw) && isNull(callback.getException())) {// unhandeled exception in @ControllerAdvice
					callback.setException(fromException(thrw));
				}
			}
			catch (Exception e) {
				context().reportError(true, "HttpSessionMonitor.postProcess", e);
			}
		}
	}
	
	public void handleError(Throwable thrw) {
		if(assertStillOpened(callback, "HttpSessionMonitor.handleError") && isNull(callback.getException())) {
			callback.setException(fromException(thrw));
		}
	}

	void emitStage(HttpAction action) {
		var end = now();
		if(assertStillOpened(callback, "StatefulMonitor.traceStep")) {
			context().emitTrace(callback.createStage(action, lastTimestamp, end, null));
		}
		lastTimestamp = end;
	}

    static URI fromRequest(HttpServletRequest req) {
    	var c = req.getRequestURL().toString();
        return create(isNull(req.getQueryString()) ? c : c + '?' + req.getQueryString());
    }
}