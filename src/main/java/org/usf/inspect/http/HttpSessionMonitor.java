package org.usf.inspect.http;

import static java.net.URI.create;
import static java.time.Clock.systemUTC;
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
import static org.usf.inspect.core.Monitor.assertStillOpened;
import static org.usf.inspect.core.Monitor.traceAtomic;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.createHttpSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.TraceDispatcherHub.hub;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.springframework.http.HttpHeaders;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpSessionSignal;
import org.usf.inspect.core.HttpSessionUpdate;
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
	private HttpSessionUpdate callback;
	private boolean async;
	
	public HttpSessionMonitor(HttpServletRequest request, HttpServletResponse response) {
		this.lastTimestamp = systemUTC().instant();
		this.handler = traceAtomic(createHttpSession(lastTimestamp, request.getHeader(TRACE_HEADER)), this::createCallback,
				ses->{
					ses.setMethod(request.getMethod());
					ses.setURI(fromRequest(request));
					ses.setAuthScheme(extractAuthScheme(request.getHeader(AUTHORIZATION))); //extract user !?
					ses.setDataSize(request.getContentLength());
					ses.setContentEncoding(request.getHeader(CONTENT_ENCODING));
					ses.setUserAgent(request.getHeader(USER_AGENT));
					if(nonNull(response)) {
						//AJOUTER POUR TESTER:
					//	response.addHeader("Via", "1.1 proxy");
					//	response.addHeader("X-Served-By", "server-01");
					//	response.addHeader("Server", "nginx/1.24");
						response.addHeader(TRACE_HEADER, ses.getId()); //add headers before doFilter
						response.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
					}
				}, 
				(call, res)->{
					if(nonNull(response)) {
						call.setStatus(response.getStatus());
						call.setDataSize(response.getBufferSize()); //!exact size
						call.setContentType(response.getContentType());
						call.setContentEncoding(response.getHeader(CONTENT_ENCODING)); 
						call.setCacheControl(response.getHeader(CACHE_CONTROL));
						call.setIntermediateNodes(extractIntermediateNodes(toHttpHeaders(response)));
					}
				});
	}
	
	//callback should be created before processing
	HttpSessionUpdate createCallback(HttpSessionSignal session) { 
		callback = session.createCallback();
		return callback;
	}
	
	public ExecutionListener<Void> preFilter(BooleanSupplier isAsync) {
		if(async && assertStillOpened(callback, "HttpSessionMonitor.preFilter")) { //!important async can be true or not set yet
			emitStage(PROCESS);
			setActiveContext(callback); //new Thread
		}
		return (s,e,o,t)-> {
			async = isAsync.getAsBoolean();
			if(async) {
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
				hub().reportError(true, "HttpSessionMonitor.postProcess", e);
			}
		}
	}
	
	public void handleError(Throwable thrw) {
		if(assertStillOpened(callback, "HttpSessionMonitor.handleError") && isNull(callback.getException())) {
			callback.setException(fromException(thrw));
		}
	}

	void emitStage(HttpAction action) {
		var end = systemUTC().instant();
		if(assertStillOpened(callback, "HttpSessionMonitor.emitStage")) {
			hub().emitTrace(callback.createStage(action, lastTimestamp, end, null));
		}
		lastTimestamp = end;
	}

    static URI fromRequest(HttpServletRequest req) {
    	var c = req.getRequestURL().toString();
        return create(isNull(req.getQueryString()) ? c : c + '?' + req.getQueryString());
    }

	private List<String> extractIntermediateNodes(HttpHeaders responseHeaders) {
		List<String> nodes = new ArrayList<>();
		addResponseIntermediateNodes(nodes, responseHeaders);
		return nodes.isEmpty() ? null : nodes;
	}

	private void addResponseIntermediateNodes(List<String> nodes, HttpHeaders responseHeaders) {
			if (nonNull(responseHeaders)) {
			String via = responseHeaders.getFirst("Via");
			if (nonNull(via) && !via.isBlank()) {
				Arrays.stream(via.split(","))
						.map(String::trim)
						.forEach(v -> nodes.add("Via: " + v));
			}

			String servedBy = responseHeaders.getFirst("X-Served-By");
			if (nonNull(servedBy) && !servedBy.isBlank()) {
				nodes.add("Served-By: " + servedBy);
			}

			String server = responseHeaders.getFirst("Server");
			if (nonNull(server) && !server.isBlank()) {
				nodes.add("Server: " + server);
			}
		}
	}


	private HttpHeaders toHttpHeaders(HttpServletResponse response) {
		var headers = new HttpHeaders();
		for(var name : response.getHeaderNames()) {
			for(var value : response.getHeaders(name)) {
				headers.add(name, value);
			}
		}
		return headers;
	}
}