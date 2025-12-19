package org.usf.inspect.http;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.HttpAction.ASSEMBLY;
import static org.usf.inspect.core.HttpAction.EXCHANGE;
import static org.usf.inspect.core.HttpAction.STREAM;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.Monitor.traceBegin;
import static org.usf.inspect.core.Monitor.traceEnd;
import static org.usf.inspect.core.Monitor.traceStep;
import static org.usf.inspect.core.SessionContextManager.createHttpRequest;

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
		ExecutionListener<Object> lstn = traceBegin(t-> 
			createHttpRequest(t, getId()),
			this::createCallback, 
			(req,o)-> fillRequest(req, client.method(), client.url(), client.headers())); //before end if thrw
		return lstn.then(traceStep(callback, (s,e,o,t)-> createStage(ASSEMBLY, s, e, t)));
	}

	public void postExchange(Throwable thrw) {
		var now = now();
		ExecutionListener<Object> lstn = traceStep(callback, (s,e,o,t)-> createStage(EXCHANGE, s, e, t));
		lstn.then(traceStep(callback, null)).safeHandle(lastTimestamp, now, null, thrw);
	}
	
	public ExecutionListener<ResponseContent> postResponse(ClientResponse response){ //read header after response
		return traceStep(callback, (s,e,res,t)-> {
			if(nonNull(res)) {
				try {
					postExchange(response.statusCode(), response.headers().asHttpHeaders());
				}
				catch (Exception ex) {
					context().reportError(true, "HttpRequestMonitor.postExchange", ex);
				}
			}
			try {
				postResponse(res);
			}
			catch (Exception ex) {
				context().reportError(true, "HttpRequestMonitor.postResponse", ex);
			}
			return createStage(STREAM, lastTimestamp, e, t);
		});
	}
		
	public void complete() {
		var now = now();
		traceEnd(callback).safeHandle(lastTimestamp, now, null, null);
	}
	
	@Override
	HttpRequestStage createStage(HttpAction action, Instant start, Instant end, Throwable thrw) {
		lastTimestamp = end; //host last stage end
		return super.createStage(action, start, end, thrw);
	}
}