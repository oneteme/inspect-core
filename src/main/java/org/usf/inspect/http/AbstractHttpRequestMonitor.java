package org.usf.inspect.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.usf.inspect.core.Callback.assertStillOpened;
import static org.usf.inspect.core.ErrorReporter.reportMessage;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.SessionContextManager.createHttpRequest;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.usf.inspect.core.HttpRequestCallback;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
class AbstractHttpRequestMonitor {
	
	@Getter private final String id = nextId();
	private HttpRequestCallback callback;

	void preExchange(Instant start, Instant end, HttpMethod method, URI uri, HttpHeaders headers, Throwable thrw) {
		var req = createHttpRequest(start, id);
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
		req.emit();
		callback = req.createCallback();
		callback.createStage(PRE_PROCESS, start, end, thrw).emit();
		if(nonNull(thrw)) { //thrw -> stage
			complete(end);
		}
	}
	
	void postExchange(Instant start, Instant end, HttpStatusCode status, HttpHeaders headers, Throwable thrw) {
//		request.setThreadName(threadName()); //deferred thread
		if(assertStillOpened(callback)) { //report if request was closed, avoid emit trace twice
			callback.createStage(PROCESS, start, end, thrw).emit();
			if(nonNull(status)) {
				callback.setStatus(status.value());
			}
			if(nonNull(headers)) { //response
				callback.setContentType(headers.getFirst(CONTENT_TYPE));
				callback.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
				callback.setLinked(assertSameID(headers.getFirst(TRACE_HEADER)));
			}
		}
	}
	
	void postResponse(Instant start, Instant end, ResponseContent cnt, Throwable thrw){
//		request.setThreadName(threadName()); //deferred thread
		if(assertStillOpened(callback)) { //report if request was closed, avoid emit trace twice
			callback.createStage(POST_PROCESS, start, end, thrw).emit();
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
	}
	
	void complete(Instant end) {
		if(assertStillOpened(callback)) { //report if request was closed, avoid emit trace twice
			callback.setEnd(end);
			callback.emit();
			callback = null;
		}
	}

	boolean assertSameID(String sid) {
		if(nonNull(sid)) {
			if(sid.equals(callback.getId())) {
				return true;
			}
			reportMessage(false, "assertSameID", "session.id=" + sid);
		}
		return false;
	}
}
