package org.usf.inspect.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpRequest2;
import org.usf.inspect.core.HttpRequestCallback;
import org.usf.inspect.core.HttpRequestStage;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
class AbstractHttpRequestMonitor {

	@Getter
	private final String id = nextId();
	
	HttpRequestCallback callback;
	
	//callback should be created before processing
	HttpRequestCallback createCallback(HttpRequest2 session) { 
		return callback = session.createCallback();
	}
	
	void fillRequest(HttpRequest2 req, HttpMethod method, URI uri, HttpHeaders headers) {
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

	void postExchange(HttpStatusCode status, HttpHeaders headers) {
//		request.setThreadName(threadName()); //deferred thread
    	if(nonNull(status)) {
			callback.setStatus(status.value());
		}
		if(nonNull(headers)) { //response
			callback.setContentType(headers.getFirst(CONTENT_TYPE));
			callback.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
			callback.setLinked(assertSameID(headers.getFirst(TRACE_HEADER)));
		}
	}
	
	void postResponse(ResponseContent cnt){
//		request.setThreadName(threadName()); //deferred thread
		if(nonNull(cnt)) {
			callback.setDataSize(cnt.contentSize());
			if(nonNull(cnt.contentBytes())) {
				callback.setBodyContent(new String(cnt.contentBytes(), UTF_8));
			}
		}
		else {
			callback.setDataSize(-1);
		}
	}
	
	HttpRequestStage createStage(HttpAction action, Instant start,Instant end, Throwable thrw) {
		return callback.createStage(action, start, end, thrw);
	}
	
	boolean assertSameID(String sid) {
		if(nonNull(sid)) {
			if(sid.equals(callback.getId())) {
				return true;
			}
			context().reportMessage(false, "assertSameID", "session.id=" + sid);
		}
		return false;
	}
}
