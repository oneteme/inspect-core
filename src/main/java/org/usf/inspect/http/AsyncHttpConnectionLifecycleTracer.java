package org.usf.inspect.http;

import static java.time.Clock.systemUTC;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.HttpAction.ASSEMBLY;
import static org.usf.inspect.core.HttpAction.EXCHANGE;
import static org.usf.inspect.core.HttpAction.STREAM;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

/**
 * 
 * @author u$f
 *
 */
final class AsyncHttpConnectionLifecycleTracer extends AbstractHttpConnectionLifecycleTracer {

	private volatile Instant lastTimestamp;
	
	public ExecutionListener<Object> assemblyStageListener(ClientRequest client) {
		return connectionListener(
				(s,e,o,t)-> createStage(ASSEMBLY, s, e), 
				(trc, req)-> signal((HttpRequestSignal)trc, client.method(), client.url(), client.headers()));
	}

	public void exchangeStage(ClientResponse res, Throwable thrw) {
		var now = systemUTC().instant();
		if(nonNull(res)) {
			try {
				traceHeaders(res.statusCode(), res.headers().asHttpHeaders());
			}
			catch (Exception ex) {
				hub().reportError(true, "HttpRequestAsyncMonitor.postExchange", ex);
			}
		}
		stageListener((s,e,o,t)-> createStage(EXCHANGE, s, e)).safeHandle(lastTimestamp, now, null, thrw);
	}
	
	public void streamStage(Instant start, Instant end, ResponseContent ctn, Throwable thrw){ //read header after response
		try {
			traceResponseContent(ctn);
		}
		catch (Exception ex) {
			hub().reportError(true, "HttpRequestAsyncMonitor.postResponse", ex);
		}
		stageListener((s,e,o,t)-> createStage(STREAM, s, e)).safeHandle(start, end, null, thrw);
	}
		
	public void complete() {
		var now = systemUTC().instant();
		disconnectionListener(null).safeHandle(lastTimestamp, now, null, null);
	}
	
	@Override
	HttpRequestStage createStage(HttpAction action, Instant start, Instant end) {
		lastTimestamp = end; //host last stage end
		return super.createStage(action, start, end);
	}
}