package org.usf.inspect.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.usf.inspect.core.HttpAction.TRANSMISSION;
import static org.usf.inspect.core.SessionContextManager.createHttpRequest;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.core.TraceHub.hub;
import static org.usf.inspect.http.WebUtils.TRACE_ID_HEADER;
import static org.usf.inspect.http.WebUtils.extractAuthScheme;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.usf.inspect.core.ConnectionLifecycleTracer;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.core.HttpRequestUpdate;
import org.usf.inspect.core.TraceSignal;
import org.usf.inspect.http.StreamCaptor.StreamExchangeListener;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor
abstract class AbstractHttpConnectionLifecycleTracer extends ConnectionLifecycleTracer implements StreamExchangeListener {

	@Getter
	private final UUID id = nextId();
	HttpRequestStage streamStage;
	
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
	        case java.net.http.HttpTimeoutException e -> CNX_TIMEOUT;
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
		var upd = (HttpRequestUpdate) getUpdate();
    	if(nonNull(status)) {
			upd.setStatus((short)status.value());
		}
		if(nonNull(headers)) { //response
			upd.setContentType(headers.getFirst(CONTENT_TYPE));
			upd.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
			upd.setLinked(assertSameID(headers.getFirst(TRACE_ID_HEADER)));
		}
		upd.setDataSize(-1); //initial size
	}
	
	void traceResponseContent(StreamCaptor cnt){
//		request.setThreadName(threadName()); //deferred thread
		if(nonNull(cnt)) {
			var upd = (HttpRequestUpdate) getUpdate();
			upd.setDataSize(cnt.transferedSize());
			if(nonNull(cnt.transferedBytes())) {
				upd.setBodyContent(new String(cnt.transferedBytes(), UTF_8));
			}
		}
	}
	
	<R> StageBuilder<R> stageBuilder(HttpAction action){
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
	
	@Override
	public void onTransmissionStart() {
		if(assertActiveTraceUpdate("AbstractHttpConnectionLifecycleTracer.onTransmissionStart")) {
			if(isNull(streamStage)) {
				var s = systemUTC().instant();
				streamStage = new HttpRequestStage(getUpdate().getId(), getStageCounter().incrementAndGet());
				streamStage.setStart(s);
				streamStage.setName(TRANSMISSION.name());
			}
			else {
				hub().emitReport("AbstractHttpConnectionLifecycleTracer.onTransmissionStart", "streamStage already started");
			}
		}
	}
	
	@Override
	public void onTransmissionEnd() {
		if(assertActiveTraceUpdate("AbstractHttpConnectionLifecycleTracer.onTransmissionEnd")) {
			if(nonNull(streamStage) && isNull(streamStage.getEnd())) {
				streamStage.setEnd(systemUTC().instant());
			}
			else {
				hub().emitReport("AbstractHttpConnectionLifecycleTracer.onTransmissionEnd", "streamStage already ended");
			}
		}
	}
	
	public void complete(StreamCaptor captor) {
		if(assertActiveTraceUpdate("AbstractHttpConnectionLifecycleTracer.complete")) {
			var now = systemUTC().instant();
			Throwable thrw = null;
			if(nonNull(captor)) {
				thrw = captor.throwable();
				try {
					traceResponseContent(captor);
				}
				catch (Exception ex) {
					hub().emitReport("AbstractHttpConnectionLifecycleTracer.complete", ex);
				}
			}
			StageBuilder<Void> stgBuilder = null;
			if(nonNull(streamStage)){
				if(isNull(streamStage.getEnd())) {
					streamStage.setEnd(now); //onTransmissionEnd may not called for some reason (e.g. connection closed by server)
				}
				stgBuilder = (s,e,o,t)-> streamStage;
			}
			disconnectionListener(stgBuilder).safeHandle(null, now, null, thrw);
		}
	}
	
	boolean assertSameID(String sid) {
		if(nonNull(sid)) {
			try {
				return getUpdate().getId().equals(fromString(sid));
			}
			catch (Exception e) {
				hub().emitReport("AbstractHttpConnectionLifecycleTracer.assertSameID", "session.id=" + sid);
			}
		}
		return false;
	}
}
