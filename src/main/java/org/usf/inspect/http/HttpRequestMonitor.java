package org.usf.inspect.http;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExecutionMonitor.runSafely;

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
		runSafely(()-> super.preProcessHandler(start, start, request.getMethod(), request.getURI(), request.getHeaders(), null));
	}

	public void postProcessHandler(Instant start, Instant end, ClientHttpResponse response, Throwable thrw) throws IOException {
		if(nonNull(response)) {
			super.postProcessHandler(start, end, response.getStatusCode(), response.getHeaders(), thrw);
		}
		else {
			super.postProcessHandler(start, end, null, null, thrw);
		}
	}
}
