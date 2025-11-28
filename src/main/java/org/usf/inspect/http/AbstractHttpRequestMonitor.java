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
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.usf.inspect.core.HttpRequestCallback;

/**
 * 
 * @author u$f
 *
 */
class AbstractHttpRequestMonitor {
	
	HttpRequestCallback callback;

	void preProcessHandler(Instant start, Instant end, HttpMethod method, URI uri, HttpHeaders headers, Throwable thrw) {
		var req = createHttpRequest(start);
		req.setMethod(method.name());
		req.setURI(uri);
		req.setAuthScheme(extractAuthScheme(headers.getFirst(AUTHORIZATION)));
		req.setDataSize(headers.getContentLength()); //-1 unknown !
		req.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
		//req.setUser(decode AUTHORIZATION)
		req.emit();
		callback = req.createCallback();
		callback.createStage(PRE_PROCESS, start, end, thrw).emit();
		if(nonNull(thrw)) { //thrw -> stage
			callback.setEnd(end);
			callback.emit();
		}
	}
	
	void postProcessHandler(Instant start, Instant end, HttpStatusCode status, HttpHeaders headers, Throwable thrw) {
//		request.setThreadName(threadName()); //deferred thread
		if(nonNull(callback)) {
			callback.createStage(PROCESS, start, end, thrw).emit();
			if(assertStillOpened(callback)) {
				if(nonNull(status)) {
					callback.setStatus(status.value());
				}
				if(nonNull(headers)) { //response
					callback.setContentType(headers.getFirst(CONTENT_TYPE));
					callback.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
					callback.setLinked(assertSameID(headers.getFirst(TRACE_HEADER)));
				}
				if(nonNull(thrw)) { //report if request was closed
					callback.setEnd(end); //thrw -> stage
					callback.emit();
				}
			}
		}
	}
	
	void completeHandler(Instant start, Instant end, ResponseContent cnt, Throwable thrw){
//		request.setThreadName(threadName()); //deferred thread
		if(nonNull(callback)) {
			callback.createStage(POST_PROCESS, start, end, thrw).emit();
			if(assertStillOpened(callback)) {
				if(nonNull(cnt)) {
					if(nonNull(cnt.contentBytes())) {
						callback.setBodyContent(new String(cnt.contentBytes(), UTF_8));
					}
					callback.setDataSize(cnt.contentSize());
				}
				else {
					callback.setDataSize(-1);
				}
				callback.setEnd(end);
				callback.emit();
			}
		}
	}

	boolean assertSameID(String sid) {
		if(nonNull(sid)) {
			if(sid.equals(callback.getId())) {
				return true;
			}
			reportMessage("assertSameID", "session.id=" + sid);
		}
		return false;
	}
	
	String getId(){
		return callback.getId();
	}
}
