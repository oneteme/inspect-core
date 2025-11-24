package org.usf.inspect.http;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 
 * @author u$f
 *
 */
final class HttpRequestMonitor extends AbstractHttpRequestMonitor {
	
	public void preProcess(HttpRequest request) {
		var start = now(); //no pre-process stage
		call(()-> super.preProcessHandler(start, start, request.getMethod(), request.getURI(), request.getHeaders(), null));
	}

	public void postProcessHandler(Instant start, Instant end, ClientHttpResponse response, Throwable thrw) throws IOException {
		callback.createStage(PROCESS, start, end, thrw).emit(); //same thread
		if(nonNull(response)) {
			super.postProcessHandler(end, response.getStatusCode(), response.getHeaders(), thrw);
		}
		else {
			super.postProcessHandler(end, null, null, thrw);
		}
	}
	
	public void completeHandler(Instant start, Instant end, ResponseContent cnt, Throwable t) {
		callback.createStage(POST_PROCESS, start, end, t).emit(); 
		super.completeHandler(end, cnt, t);
	}
}
