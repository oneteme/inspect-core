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
import org.usf.inspect.core.ConnectionLifecycleTracer;
import org.usf.inspect.core.DualEventTracer;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.core.HttpRequestUpdate;
import org.usf.inspect.core.TraceSignal;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor
abstract class AbstractHttpConnectionLifecycleTracer extends ConnectionLifecycleTracer {

	@Getter
	private final UUID id = nextId();
	
	@Override
	protected TraceSignal signal(Instant start) {
		return createHttpRequest(start, getId());
	}

	@Override
	protected HttpRequestUpdate update(TraceSignal signal) { 
		return new HttpRequestUpdate(signal.getId());
	}

	@Override
	public short resolveStatus(Throwable t) {
	    return switch (t) {
	        case java.net.http.HttpConnectTimeoutException e -> CONN_TIMEOUT;
	        case java.net.http.HttpTimeoutException e -> SERVER_TIMEOUT;

	        default -> super.resolveStatus(t);
	    };
	}
	
	protected static HttpRequestSignal signal(HttpRequestSignal sgn, HttpMethod method, URI uri, HttpHeaders headers) {
		if(nonNull(method)) {
			sgn.setMethod(method.name());
		}
		if(nonNull(uri)) {
			sgn.setProtocol(uri.getScheme());
			sgn.setHost(uri.getHost());
			sgn.setPort(uri.getPort());
			sgn.setPath(uri.getPath());
			sgn.setQuery(uri.getQuery());
		}
		if(nonNull(headers)) {
			sgn.setAuthScheme(extractAuthScheme(headers.getFirst(AUTHORIZATION)));
			sgn.setDataSize(headers.getContentLength()); //-1 unknown !
			sgn.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
			//req.setUser(decode AUTHORIZATION)
		}
		return sgn;
	}

	void traceHeaders(HttpStatusCode status, HttpHeaders headers) {
//		request.setThreadName(threadName()); //deferred thread
		if(assertActiveTraceUpdate("AbstractHttpConnectionLifecycleTracer.traceHeaders")) {
			var upd = (HttpRequestUpdate) getUpdate();
	    	if(nonNull(status)) {
				upd.setStatus((short)status.value());
			}
			if(nonNull(headers)) { //response
				upd.setContentType(headers.getFirst(CONTENT_TYPE));
				upd.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
				upd.setLinked(assertSameID(headers.getFirst(TRACE_HEADER)));
			}
			upd.setDataSize(-1); //initial size
		}
	}
	
	void traceResponseContent(ResponseContent cnt){
//		request.setThreadName(threadName()); //deferred thread
		if(assertActiveTraceUpdate("AbstractHttpConnectionLifecycleTracer.traceResponseContent")) {
			var upd = (HttpRequestUpdate) getUpdate();
			if(nonNull(cnt)) {
				upd.setDataSize(cnt.contentSize());
				if(nonNull(cnt.contentBytes())) {
					upd.setBodyContent(new String(cnt.contentBytes(), UTF_8));
				}
			}
		}
	}
	
	<R> DualEventTracer.StageBuilder<R> stageBuilder(HttpAction action){
		return (s,e,o,t)-> createStage(action, s, e);
	}
	
	HttpRequestStage createStage(HttpAction action, Instant start, Instant end) {
		var stg = new HttpRequestStage(getUpdate().getId(), getStageCounter().incrementAndGet());
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
				return getUpdate().getId().equals(fromString(sid));
			}
			catch (Exception e) {
				hub().reportMessage(false, "AbstractHttpConnectionLifecycleTracer.assertSameID", "session.id=" + sid);
			}
		}
		return false;
	}
}
