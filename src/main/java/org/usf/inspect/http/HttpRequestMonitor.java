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
import org.usf.inspect.core.RestRequest;

/**
 * 
 * @author u$f
 *
 */
final class HttpRequestMonitor extends AbstractHttpRequestMonitor {
	
	public void preProcess(HttpRequest request) {
		var start = now();
		call(()-> super.preProcessHandler(start, start, request.getMethod(), request.getURI(), request.getHeaders(), null));
	}

	public RestRequest postProcessHandler(Instant start, Instant end, ClientHttpResponse response, Throwable thrw) throws IOException {
		request.createStage(PROCESS, start, end, thrw).emit(); //same thread
		return nonNull(response)
				? super.postProcessHandler(end, response.getStatusCode(), response.getHeaders(), thrw)
				: super.postProcessHandler(end, null, null, thrw);
	}
	
	public RestRequest completeHandler(Instant start, Instant end, ResponseContent cnt, Throwable t) {
		request.createStage(POST_PROCESS, start, end, t).emit(); 
		return super.completeHandler(end, cnt, t);
	}
}
