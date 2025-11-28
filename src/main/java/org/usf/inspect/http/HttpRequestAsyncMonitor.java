package org.usf.inspect.http;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExecutionMonitor.runSafely;

import java.time.Instant;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;

/**
 * 
 * @author u$f
 *
 */
final class HttpRequestAsyncMonitor extends AbstractHttpRequestMonitor {

	private Instant lastTimestamp;
	
	public ExecutionHandler<Object> preProcessHandler(ClientRequest req) {
		return (s,e,o,t)-> {
			super.preProcessHandler(s, e, req.method(), req.url(), req.headers(), t);
			lastTimestamp = e;
		};
	}

	public void postProcess(ClientResponse res, Throwable thrw) {
		var now = now();
		runSafely(()->{
			if(nonNull(res)) {
				super.postProcessHandler(lastTimestamp, now, res.statusCode(), res.headers().asHttpHeaders(), thrw);
			}
			else {
				super.postProcessHandler(lastTimestamp, now, null, null, thrw);
			}
		});
		lastTimestamp = now;
	}
	
	public void completeHandler(Instant end, ResponseContent cnt, Throwable t) {
		super.completeHandler(lastTimestamp, end, cnt, t);
	}
}