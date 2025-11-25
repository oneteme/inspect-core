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
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.HttpAction.DEFERRED;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.SessionContextManager.createHttpSession;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;

import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpSessionCallback;
import org.usf.inspect.core.HttpSessionStage;
import org.usf.inspect.core.SessionContext;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 
 * @author u$f 
 *
 */
public final class HttpSessionMonitor {
	
	private HttpSessionCallback call;
	private SessionContext ctx;
	private Instant lastTimestamp;
	
	public void preFilter(HttpServletRequest req, HttpServletResponse res){
		lastTimestamp = now();
		var ses = createHttpSession(lastTimestamp, req.getHeader(TRACE_HEADER));
		call(()->{
			ses.setMethod(req.getMethod());
			ses.setURI(fromRequest(req));
			ses.setAuthScheme(extractAuthScheme(req.getHeader(AUTHORIZATION))); //extract user !?
			ses.setDataSize(req.getContentLength());
			ses.setContentEncoding(req.getHeader(CONTENT_ENCODING));
			ses.setUserAgent(req.getHeader(USER_AGENT));
			ses.emit();
			res.addHeader(TRACE_HEADER, ses.getId()); //add headers before doFilter
			res.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
		});
		call = ses.createCallback();
		ctx = call.setupContext();
	}
	
	public ExecutionHandler<Void> asyncFilterHandler(){
		ctx.setup(); //re-setup context for defered processing
		return (s,e,o,t)-> {
			createStage(DEFERRED, e).emit();
			ctx.release(); //release context after defered processing
		};
	}
	
	public void preProcess(){
		call(()-> createStage(PRE_PROCESS, now()).emit());
	}
	
	public void process(){
		call(()-> createStage(PROCESS, now()).emit());
	}

	public void postProcess(String name, String user, Throwable thrw){
		call(()-> {
			createStage(POST_PROCESS, now()).emit();
			call.setName(name);
			call.setUser(user);
			if(nonNull(thrw) && isNull(call.getException())) {// unhandeled exception in @ControllerAdvice
				call.setException(fromException(thrw));
			}
		});
	}
	
	public void handleError(Throwable thrw) {
		if(isNull(call.getException())) {
			call.setException(fromException(thrw));
		}
	}
	
	public void postFilterHandler(Instant end, HttpServletResponse response, Throwable thrw) {
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
		ctx.release();
	}

	HttpSessionStage createStage(HttpAction action, Instant end) {
		var now = now();
		var stg = call.createStage(action, lastTimestamp, end, null);
		lastTimestamp = now;
		return stg;
	}

    static URI fromRequest(HttpServletRequest req) {
    	var c = req.getRequestURL().toString();
        return create(isNull(req.getQueryString()) ? c : c + '?' + req.getQueryString());
    }

}