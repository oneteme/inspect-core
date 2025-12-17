package org.usf.inspect.http;

import static java.time.Instant.now;
import static java.util.Map.entry;
import static java.util.Objects.nonNull;

import java.time.Instant;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

/**
 * 
 * @author u$f
 *
 */
final class HttpRequestAsyncMonitor extends AbstractHttpRequestMonitor {

	private volatile Instant lastTimestamp;
	
	public ExecutionListener<Object> preProcessHandler(ClientRequest req) {
		return (s,e,o,t)-> {
			super.preExchange(req.method(), req.url(), req.headers()).handle(s, e, null, t);
			lastTimestamp = e;
		};
	}

	public void postExchange(ClientResponse res, Throwable thrw) {
		var now = now();
		var entry = nonNull(res) ? entry(res.statusCode(), res.headers().asHttpHeaders()) : null;
		super.postExchange().fire(lastTimestamp, now, entry, thrw);
		lastTimestamp = now;
	}
		
	public void complete() {
		super.disconnection().fire(lastTimestamp, now(), null, null);
	}
}