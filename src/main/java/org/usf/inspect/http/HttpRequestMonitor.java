package org.usf.inspect.http;

import static java.time.Instant.now;
import static java.util.Map.entry;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExecutionMonitor.notifyHandler;

import java.time.Instant;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;

/**
 * 
 * @author u$f
 *
 */
final class HttpRequestMonitor extends AbstractHttpRequestMonitor {
	
	public void preExchange(HttpRequest request) { //no pre-process 
		var start = now();
		notifyHandler(super.preExchange(request.getMethod(), request.getURI(), request.getHeaders()), start, start, null, null);
	}

	public ExecutionHandler<ClientHttpResponse> clientHttpResponseHandler() {
		return (s,e,res,t)-> {
			if(nonNull(res)) {
				super.postExchange().handle(s, e, entry(res.getStatusCode(), res.getHeaders()), t);
			}
			if(nonNull(t)) {
				super.disconnection().handle(s, e, null, null);
			}
		};
	}
	
	ExecutionHandler<ResponseContent> postResponse(Instant start, Instant end, ResponseContent cnt, Throwable thrw) {
		return (s,e,o,t)-> {
			notifyHandler(super.postResponse(), start, end, cnt, thrw);
			super.disconnection().handle(s, e, null, null);
		};
	}
}
