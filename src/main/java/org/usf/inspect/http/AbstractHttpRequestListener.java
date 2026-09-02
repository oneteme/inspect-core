package org.usf.inspect.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.SessionContextManager.createHttpRequest;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.core.TraceDispatcherHub.hub;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.core.HttpRequestUpdate;
import org.usf.inspect.core.Monitor.StageBuilder;
import org.usf.inspect.core.StatefulExecutionListener;
import org.usf.inspect.core.TraceSignal;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor
abstract class AbstractHttpRequestListener extends StatefulExecutionListener {

	@Getter
	private final UUID id = nextId();
	
	@Override
	public TraceSignal signal(Instant start) {
		return createHttpRequest(start, getId());
	}

	@Override
	public HttpRequestUpdate update(TraceSignal signal) { 
		return new HttpRequestUpdate(signal.getId());
	}

	@Override
	public int resolveStatus(Throwable t) {
	    return switch (t) {
	        case java.net.http.HttpConnectTimeoutException e -> CONN_TIMEOUT;
	        case java.net.http.HttpTimeoutException e -> SERVER_TIMEOUT;

	        default -> super.resolveStatus(t);
	    };
	}
	
	protected static HttpRequestSignal signal(HttpRequestSignal sng, HttpMethod method, URI uri, HttpHeaders headers) {
		if(nonNull(method)) {
			sng.setMethod(method.name());
		}
		if(nonNull(uri)) {
			sng.setProtocol(uri.getScheme());
			sng.setHost(uri.getHost());
			sng.setPort(uri.getPort());
			sng.setPath(uri.getPath());
			sng.setQuery(uri.getQuery());
		}
		if(nonNull(headers)) {
			sng.setAuthScheme(extractAuthScheme(headers.getFirst(AUTHORIZATION)));
			sng.setDataSize(headers.getContentLength()); //-1 unknown !
			sng.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
			//req.setUser(decode AUTHORIZATION)
		}
		return sng;
	}

	void traceHeaders(HttpStatusCode status, HttpHeaders headers) {
//		request.setThreadName(threadName()); //deferred thread
		var upd = (HttpRequestUpdate) getTrace();
		if(nonNull(upd)) {
	    	if(nonNull(status)) {
				upd.setStatus(status.value());
			}
			if(nonNull(headers)) { //response
				upd.setContentType(headers.getFirst(CONTENT_TYPE));
				upd.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
				upd.setLinked(assertSameID(headers.getFirst(TRACE_HEADER)));
			}
			upd.setDataSize(-1); //initial size
		}
		else {
			reportTraceIsNull("update");
		}
	}
	
	void traceResponseContent(ResponseContent cnt){
//		request.setThreadName(threadName()); //deferred thread
		var upd = (HttpRequestUpdate) getTrace();
		if(nonNull(upd)) {
			if(nonNull(cnt)) {
				upd.setDataSize(cnt.contentSize());
				if(nonNull(cnt.contentBytes())) {
					upd.setBodyContent(new String(cnt.contentBytes(), UTF_8));
				}
			}
		}
		else {
			reportTraceIsNull("update");
		}
	}
	
	<R> StageBuilder<R> stageBuilder(HttpAction action){
		return (s,e,o,t)-> createStage(action, s, e);
	}
	
	HttpRequestStage createStage(HttpAction action, Instant start, Instant end) {
		var stg = new HttpRequestStage(getTrace().getId(), getStageCounter().incrementAndGet());
		stg.setName(action.name());
		stg.setStart(start);
		stg.setEnd(end);
//		stg.setCommand(null)
//		stg.setPayload(null)
		return stg;
	}
	
	boolean assertSameID(String sid) {
		if(nonNull(sid)) {
			try {
				return getTrace().getId().equals(fromString(sid));
			}
			catch (Exception e) {
				//do nothing
			}
			hub().reportMessage(false, "assertSameID", "session.id=" + sid);
		}
		return false;
	}
}
