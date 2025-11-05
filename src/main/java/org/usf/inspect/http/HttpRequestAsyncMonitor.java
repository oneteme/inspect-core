package org.usf.inspect.http;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;

import java.time.Instant;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpRequestStage;

/**
 * 
 * @author u$f
 *
 */
final class HttpRequestAsyncMonitor extends AbstractHttpRequestMonitor {

	private Instant lastTimestamp;
	
	public ExecutionHandler<Object> preProcessHandler(ClientRequest req) {
		return (s,e,o,t)-> {
			createStage(PRE_PROCESS, s, e, t).emit(); //emit manually
			super.preProcessHandler(s, e, req.method(), req.url(), req.headers(), t);
		};
	}

	public void postProcess(ClientResponse res, Throwable thrw) {
		var now = now();
		call(()->{
			createStage(PROCESS, lastTimestamp, now, thrw).emit(); //emit manually
			if(nonNull(res)) {
				super.postProcessHandler(now, res.statusCode(), res.headers().asHttpHeaders(), thrw);
			}
			else {
				super.postProcessHandler(now, null, null, thrw);
			}
		});
	}
	
	public void completeHandler(Instant start, Instant end, ResponseContent cnt, Throwable t) {
		createStage(POST_PROCESS, lastTimestamp, end, t).emit();
		super.completeHandler(end, cnt, t);
	}

	HttpRequestStage createStage(HttpAction action, Instant start, Instant end, Throwable thrw) {
		var stg = request.createStage(action, start, end, thrw);
		lastTimestamp = end;
		return stg;
	}
}