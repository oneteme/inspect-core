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
 * 
 * @author u$f
 *
 */
final class HttpRequestAsyncMonitor extends AbstractHttpRequestMonitor {

	private volatile Instant lastTimestamp;
	
	public ExecutionListener<Object> preExchange(ClientRequest client) {
		return traceBegin(t-> 
			createHttpRequest(t, getId()),
			(req,o)-> fillRequest(req, client.method(), client.url(), client.headers()), //before end if thrw
			traceStep((s,e,o,t)-> createStage(ASSEMBLY, s, e, t)));
	}

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
	
	public void postResponse(Instant start, Instant end, ResponseContent ctn, Throwable thrw){ //read header after response
		try {
			super.postResponse(ctn);
		}
		catch (Exception ex) {
			hub().reportError(true, "HttpRequestMonitor.postResponse", ex);
		}
		traceStep((s,e,o,t)-> createStage(STREAM, s, e, t)).safeHandle(start, end, null, thrw);
	}
		
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