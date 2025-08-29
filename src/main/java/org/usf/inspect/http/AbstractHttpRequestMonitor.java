package org.usf.inspect.http;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.usf.inspect.core.ErrorReporter.reporter;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.createHttpRequest;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.usf.inspect.core.RestRequest;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
class AbstractHttpRequestMonitor {
	
	@Getter
	final RestRequest request = createHttpRequest();

	RestRequest preProcessHandler(Instant start, Instant end, HttpMethod method, URI uri, HttpHeaders headers, Throwable thrw) throws IOException {
		request.setStart(start);
		request.setThreadName(threadName());
		request.setMethod(method.name());
		request.setURI(uri);
		request.setAuthScheme(extractAuthScheme(headers.getFirst(AUTHORIZATION)));
		request.setOutDataSize(headers.getContentLength()); //-1 unknown !
		request.setOutContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
		//req.setUser(decode AUTHORIZATION)
		if(nonNull(thrw)) { //thrw -> stage
			request.setEnd(end);
		}
		return request;
	}
	
	RestRequest postProcessHandler(Instant end, HttpStatusCode status, HttpHeaders headers, Throwable thrw) throws IOException {
		request.runSynchronized(()->{
			request.setThreadName(threadName()); //deferred thread
			if(nonNull(status)) {
				request.setStatus(status.value());
			}
			if(nonNull(headers)) { //response
				request.setContentType(headers.getFirst(CONTENT_TYPE));
				request.setInContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
				assertSameID(headers.getFirst(TRACE_HEADER));
			}
			if(nonNull(thrw)) { //thrw -> stage
				request.setEnd(end);
			}
		});
		return nonNull(thrw) ? request : null;
	}
	
	RestRequest completeHandler(Instant end, ResponseContent cnt, Throwable t){
		request.runSynchronized(()->{
			if(nonNull(cnt)) {
				request.setBodyContent(new String(cnt.contentBytes(), UTF_8));
				request.setInDataSize(cnt.contentSize());
			}
			else {
				request.setInDataSize(-1);
			}
			request.setEnd(end);
		});
		return request;
	}

	void assertSameID(String sessionID) {
		if(nonNull(sessionID) && !sessionID.equals(request.getId())) {
			reporter(false).action("assertSameID")
			.message(format("req.id='%s', ses.id='%s'", request.getId(), sessionID))
			.trace(request).emit();
		}
	}
}
