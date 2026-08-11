package org.usf.inspect.http;

import static java.time.Clock.systemUTC;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.HttpAction.ASSEMBLY;
import static org.usf.inspect.core.HttpAction.EXCHANGE;
import static org.usf.inspect.core.HttpAction.STREAM;
import static org.usf.inspect.core.SessionContextManager.createHttpRequest;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

/**
 * Tracks reactive client HTTP request execution and response streaming stages.
 */
final class HttpRequestAsyncMonitor extends AbstractHttpRequestMonitor {

	private volatile Instant lastTimestamp;
	
	/**
	 * Creates a listener that records request assembly before the exchange begins.
	 *
	 * @param client the outgoing reactive client request
	 * @return the listener that starts request monitoring
	 */
	public ExecutionListener<Object> preExchange(ClientRequest client) {
		return traceBegin(t-> 
			createHttpRequest(t, getId()),
			(req,o)-> fillRequest(req, client.method(), client.url(), client.headers()), //before end if thrw
			traceStep((s,e,o,t)-> createStage(ASSEMBLY, s, e, t)));
	}

	/**
	 * Records exchange completion details and an optional failure.
	 *
	 * @param res the received client response, if any
	 * @param thrw the failure raised during exchange execution, if any
	 */
	public void postExchange(ClientResponse res, Throwable thrw) {
		var now = systemUTC().instant();
		if(nonNull(res)) {
			try {
				postExchange(res.statusCode(), res.headers().asHttpHeaders());
			}
			catch (Exception ex) {
				hub().reportError(true, "HttpRequestMonitor.postExchange", ex);
			}
		}
		traceStep((s,e,o,t)-> createStage(EXCHANGE, s, e, t)).safeHandle(lastTimestamp, now, null, thrw);
	}
	
	/**
	 * Records streamed response content for the supplied time range.
	 *
	 * @param start the response streaming start time
	 * @param end the response streaming end time
	 * @param ctn the captured response content
	 * @param thrw the failure raised while streaming the response, if any
	 */
	public void postResponse(Instant start, Instant end, ResponseContent ctn, Throwable thrw){ //read header after response
		try {
			super.postResponse(ctn);
		}
		catch (Exception ex) {
			hub().reportError(true, "HttpRequestMonitor.postResponse", ex);
		}
		traceStep((s,e,o,t)-> createStage(STREAM, s, e, t)).safeHandle(start, end, null, thrw);
	}
		
	/**
	 * Completes the current reactive HTTP request trace.
	 */
	public void complete() {
		var now = systemUTC().instant();
		traceEnd(null).safeHandle(lastTimestamp, now, null, null);
	}
	
	@Override
	HttpRequestStage createStage(HttpAction action, Instant start, Instant end, Throwable thrw) {
		lastTimestamp = end; //host last stage end
		return super.createStage(action, start, end, thrw);
	}
}