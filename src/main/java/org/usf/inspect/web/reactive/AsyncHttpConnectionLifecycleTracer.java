package org.usf.inspect.web.reactive;

import static java.time.Clock.systemUTC;
import static java.util.Objects.nonNull;
import static org.usf.inspect.HttpAction.EXECUTION;
import static org.usf.inspect.HttpAction.INITIALIZATION;
import static org.usf.inspect.core.Constants.REPORTER;

import java.time.Instant;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.usf.inspect.HttpAction;
import org.usf.inspect.HttpRequestSignal;
import org.usf.inspect.HttpRequestStage;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.web.AbstractHttpConnectionLifecycleTracer;

/**
 * 
 * @author u$f
 *
 */
final class AsyncHttpConnectionLifecycleTracer extends AbstractHttpConnectionLifecycleTracer {

	private volatile Instant lastTimestamp;
	
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
					REPORTER.error("AsyncHttpConnectionLifecycleTracer.exchangeStage: failed to trace headers", ex);
				}
			}
			stageListener((s,e,o,t)-> createStage(EXECUTION, s, e)).safeHandle(lastTimestamp, now, null, thrw);
		}
	}
	
	@Override
	protected HttpRequestStage createStage(HttpAction action, Instant start, Instant end) {
		lastTimestamp = end; //host last stage end
		return super.createStage(action, start, end);
	}
}