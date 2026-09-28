package org.usf.inspect.http;

import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.HttpAction.EXECUTION;
import static org.usf.inspect.core.HttpAction.INITIALIZATION;
import static org.usf.inspect.core.HttpAction.TRANSMISSION;
import static org.usf.inspect.core.TraceHub.hub;

import java.time.Instant;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.http.StreamCaptor.StreamExchangeListener;

/**
 * 
 * @author u$f
 *
 */
final class AsyncHttpConnectionLifecycleTracer extends AbstractHttpConnectionLifecycleTracer implements StreamExchangeListener {

	private volatile Instant lastTimestamp;
	private HttpRequestStage streamStage;
	
	public ExecutionListener<Object> assemblyStageListener(ClientRequest client) {
		return connectionListener(
				(s,e,o,t)-> createStage(INITIALIZATION, s, e), 
				(trc, req)-> signal((HttpRequestSignal)trc, client.method(), client.url(), client.headers()));
	}

	public void exchangeStage(ClientResponse res, Throwable thrw) {
		if(assertActiveTraceUpdate("AsyncHttpConnectionLifecycleTracer.exchangeStage")) {
			var now = systemUTC().instant();
			if(nonNull(res)) {
				try {
					traceHeaders(res.statusCode(), res.headers().asHttpHeaders());
				}
				catch (Exception ex) {
					hub().reportError("AsyncHttpConnectionLifecycleTracer.exchangeStage", ex);
				}
			}
			stageListener((s,e,o,t)-> createStage(EXECUTION, s, e)).safeHandle(lastTimestamp, now, null, thrw);
		}
	}
	
	public void complete(StreamCaptor payload) {
		if(assertActiveTraceUpdate("AsyncHttpConnectionLifecycleTracer.complete")) {
			var now = systemUTC().instant();
			Throwable thrw = null;
			if(nonNull(payload)) {
				thrw = payload.throwable();
				try {
					traceResponseContent(payload);
				}
				catch (Exception ex) {
					hub().reportError("AsyncHttpConnectionLifecycleTracer.complete", ex);
				}
			}
			StageBuilder<Void> stgBuilder = null;
			if(nonNull(streamStage)){
				if(isNull(streamStage.getEnd())) {
					streamStage.setEnd(now); //onTransmissionEnd may not called for some reason (e.g. connection closed by server)
				}
				stgBuilder = (s,e,o,t)-> streamStage;
			}
			disconnectionListener(stgBuilder).safeHandle(lastTimestamp, now, null, thrw);
		}
	}
	
	@Override
	HttpRequestStage createStage(HttpAction action, Instant start, Instant end) {
		lastTimestamp = end; //host last stage end
		return super.createStage(action, start, end);
	}
	
	@Override
	public void onTransmissionStart() {
		if(assertActiveTraceUpdate("AsyncHttpConnectionLifecycleTracer.onTransmissionStart")) {
			if(isNull(streamStage)) {
				var s = systemUTC().instant();
				streamStage = new HttpRequestStage(getUpdate().getId(), getStageCounter().incrementAndGet());
				streamStage.setStart(s);
				streamStage.setName(TRANSMISSION.name());
			}
			else {
				hub().reportMessage("AsyncHttpConnectionLifecycleTracer.onTransmissionStart", "streamStage already started");
			}
		}
	}
	
	@Override
	public void onTransmissionEnd() {
		if(assertActiveTraceUpdate("AsyncHttpConnectionLifecycleTracer.onTransmissionEnd")) {
			if(nonNull(streamStage) && isNull(streamStage.getEnd())) {
				streamStage.setEnd(systemUTC().instant());
			}
			else {
				hub().reportMessage("AsyncHttpConnectionLifecycleTracer.onTransmissionEnd", "streamStage already ended");
			}
		}
	}
}