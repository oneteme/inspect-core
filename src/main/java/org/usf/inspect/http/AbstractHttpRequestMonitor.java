package org.usf.inspect.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.usf.inspect.core.ExceptionInfo.fromException2;
import static org.usf.inspect.core.ExceptionInfo.rootCauseException;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.RequestCommonStatus.CONN_TIMEOUT;
import static org.usf.inspect.core.RequestCommonStatus.SERVER_TIMEOUT;
import static org.usf.inspect.core.RequestCommonStatus.SUCCESS;
import static org.usf.inspect.core.RequestCommonStatus.statusFor;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.core.TraceDispatcherHub.hub;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.core.HttpRequestUpdate;
import org.usf.inspect.core.Monitor.StatefulMonitor;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
class AbstractHttpRequestMonitor extends StatefulMonitor<HttpRequestSignal, HttpRequestUpdate> {

	@Getter
	private final String id = nextId();
	
	protected HttpRequestUpdate createCallback(HttpRequestSignal session) { 
		return session.createCallback();
	}
	
	void fillRequest(HttpRequestSignal req, HttpMethod method, URI uri, HttpHeaders headers) {
		if(nonNull(method)) {
			req.setMethod(method.name());
		}
		if(nonNull(uri)) {
			req.setURI(uri);
		}
		if(nonNull(headers)) {
			req.setAuthScheme(extractAuthScheme(headers.getFirst(AUTHORIZATION)));
			req.setDataSize(headers.getContentLength()); //-1 unknown !
			req.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
			//req.setUser(decode AUTHORIZATION)
		}
	}

	void postExchange(HttpStatusCode status, HttpHeaders headers, Throwable thrw) {
//		request.setThreadName(threadName()); //deferred thread
		var callback = getCallback();
    	if(nonNull(status)) {
			callback.setStatus(status.value());
		}
    	else if(nonNull(thrw)) {
    		callback.setStatus(resolveStatus(thrw));
		}
		if(nonNull(headers)) { //response
			callback.setContentType(headers.getFirst(CONTENT_TYPE));
			callback.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
			callback.setLinked(assertSameID(headers.getFirst(TRACE_HEADER)));
		}
		callback.setDataSize(-1); //reset size before streaming
	}
	
	void postResponse(ResponseContent cnt){
//		request.setThreadName(threadName()); //deferred thread
		if(nonNull(cnt)) {
			var callback = getCallback();
			callback.setDataSize(cnt.contentSize());
			if(nonNull(cnt.contentBytes())) {
				callback.setBodyContent(new String(cnt.contentBytes(), UTF_8));
			}
		}
	}
	
	HttpRequestStage createStage(HttpAction action, Instant start,Instant end, Throwable thrw) {
		var upd = getCallback();
		var stg = upd.createStage();
		stg.setName(action.name());
		stg.setStart(start);
		stg.setEnd(end);
//		if(nonNull(cmd)) {
//			stg.setCommand(cmd.name());
//			upd.setCommand(merge(upd.getCommand(), cmd.getType()));
//		}
		if(nonNull(thrw)) {
			var root = rootCauseException(thrw);
//			upd.setStatus(resolveStatus(root));
			stg.setException(fromException2(root));
		}
		else {
			upd.setStatus(SUCCESS);
		}
		return stg;
	}
	
	boolean assertSameID(String sid) {
		if(nonNull(sid)) {
			if(sid.equals(getCallback().getId())) {
				return true;
			}
			hub().reportMessage(false, "assertSameID", "session.id=" + sid);
		}
		return false;
	}

	static int resolveStatus(Throwable t) {
	    if (isNull(t)) {
	        return SUCCESS;
	    }
	    return switch (t) {
	        case java.net.http.HttpConnectTimeoutException e -> CONN_TIMEOUT;
	        case java.net.http.HttpTimeoutException e -> SERVER_TIMEOUT;

	        default -> statusFor(t);
	    };
	}
}
