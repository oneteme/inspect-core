package org.usf.inspect.http;

import static java.net.URI.create;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.USER_AGENT;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.HttpAction.DEFERRED;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.SessionManager.createRestSession;

import java.net.URI;
import java.time.Instant;

import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpSessionStage;
import org.usf.inspect.core.HttpUserProvider;
import org.usf.inspect.core.RestSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

/**
 * 
 * @author u$f 
 *
 */
public final class HttpSessionMonitor {
	
	@Getter
	private final RestSession session;
	private final HttpServletRequest req;
	private Instant lastTimestamp;
	
	public HttpSessionMonitor(HttpServletRequest req, String id) {
		this.req = req;
		if(nonNull(id)) {
			this.session = createRestSession(id);
			this.session.setLinked(true);
		}
		else {
			this.session = createRestSession();
		}
	}
	
	public void preFilter(){
		lastTimestamp = now();
		call(()->{
			session.setStart(lastTimestamp);
			session.setThreadName(threadName());
			session.setMethod(req.getMethod());
			session.setURI(fromRequest(req));
			session.setAuthScheme(extractAuthScheme(req.getHeader(AUTHORIZATION))); //extract user !?
			session.setInDataSize(req.getContentLength());
			session.setInContentEncoding(req.getHeader(CONTENT_ENCODING));
			session.setUserAgent(req.getHeader(USER_AGENT));
			return session.updateContext();
		});
	}
	
	public void preProcess(){
		call(()-> createStage(PRE_PROCESS, now()));
	}
	
	public void process(){
		call(()-> createStage(PROCESS, now()));
	}

	public void postProcess(String name, HttpUserProvider userProvider, Throwable thrw){
		call(()-> {
			createStage(POST_PROCESS, now()).emit();
			session.runSynchronized(()->{
				session.setName(name);
				session.setUser(userProvider.getUser(req, name));
				if(nonNull(thrw) && isNull(session.getException())) {// unhandeled exception in @ControllerAdvice
					session.setException(fromException(thrw));
				}
			});
			return null; //do not emit session
		});
	}
	
	public RestSession postFilterHandler(boolean async, Instant end, HttpServletResponse response, Throwable thrw) {
		if(async) {
			createStage(DEFERRED, end).emit();
		}
		session.runSynchronized(()->{
			if(!async && nonNull(response)) {
				session.setStatus(response.getStatus());
				session.setOutDataSize(response.getBufferSize()); //!exact size
				session.setOutContentEncoding(response.getHeader(CONTENT_ENCODING)); 
				session.setCacheControl(response.getHeader(CACHE_CONTROL));
				session.setContentType(response.getContentType());
			}
			if(nonNull(thrw) && isNull(session.getException())) { // see advise & interceptor
				session.setException(fromException(thrw));
			}
			session.setEnd(end);  //IO | CancellationException | ServletException => no ErrorHandler
		});
		return !async || nonNull(thrw) ? session.releaseContext() : null;
	}

	HttpSessionStage createStage(HttpAction action, Instant end) {
		var now = now();
		var stg = session.createStage(action, lastTimestamp, end, null);
		lastTimestamp = now;
		return stg;
	}

    static URI fromRequest(HttpServletRequest req) {
    	var c = req.getRequestURL().toString();
        return create(isNull(req.getQueryString()) ? c : c + '?' + req.getQueryString());
    }
}