package org.usf.inspect.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.usf.inspect.core.Helper.extractAuthScheme;
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
 * Provides shared request tracing support for outbound HTTP monitors.
 */
class AbstractHttpRequestMonitor extends StatefulMonitor<HttpRequestSignal, HttpRequestUpdate> {

	@Getter
	private final String id = nextId();
	
	/**
	 * Creates the callback update associated with the current HTTP request session.
	 *
	 * @param session the HTTP request session signal
	 * @return the callback update for the session
	 */
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
		}
	}

	/***
	 * Posts the exchange information to the current callback, updating its status, content type, content encoding, and linked trace ID if available.
	 * @param status - HttpStatusCode - the HTTP status code of the response
	 * @param headers - HttpHeaders - the HTTP headers of the response
	 */
	void postExchange(HttpStatusCode status, HttpHeaders headers) {
		var callback = getCallback();
    	if(nonNull(status)) {
			callback.setStatus(status.value());
		}
		if(nonNull(headers)) { //response
			callback.setContentType(headers.getFirst(CONTENT_TYPE));
			callback.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
			callback.setLinked(assertSameID(headers.getFirst(TRACE_HEADER)));
		}
		callback.setDataSize(-1); //reset size before streaming
	}

	/**
	 * Posts the response content to the current callback, updating its data size and body content if available.
	 * @param cnt - ResponseContent - object containing the response data
	 */
	void postResponse(ResponseContent cnt){
		if(nonNull(cnt)) {
			var callback = getCallback();
			callback.setDataSize(cnt.contentSize());
			if(nonNull(cnt.contentBytes())) {
				callback.setBodyContent(new String(cnt.contentBytes(), UTF_8));
			}
		}
	}
	
	HttpRequestStage createStage(HttpAction action, Instant start,Instant end, Throwable thrw) {
		return getCallback().createStage(action, start, end, thrw);
	}

	/**
	 * Asserts that the provided session ID matches the current callback's ID.
	 * @param sid
	 * @return
	 */
	boolean assertSameID(String sid) {
		if(nonNull(sid)) {
			if(sid.equals(getCallback().getId())) {
				return true;
			}
			hub().reportMessage(false, "assertSameID", "session.id=" + sid);
		}
		return false;
	}
}
