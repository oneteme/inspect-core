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
	
	public void preExchange(HttpRequest request) { //no pre-process 
		var start = now();
		runSafely(()-> super.preExchange(start, start, request.getMethod(), request.getURI(), request.getHeaders(), null));
	}

	public void postExchange(Instant start, Instant end, ClientHttpResponse response, Throwable thrw) throws IOException {
		if(nonNull(response)) {
			super.postExchange(start, end, response.getStatusCode(), response.getHeaders(), thrw);
		}
		else {
			super.postExchange(start, end, null, null, thrw);
		}
		if(nonNull(thrw)) {
			super.complete(end); //sync client, complete on error
		}
	}
	
	@Override
	void postResponse(Instant start, Instant end, ResponseContent cnt, Throwable thrw) {
		super.postResponse(start, end, cnt, thrw);
		super.complete(end); //sync client, complete after response
	}
}
