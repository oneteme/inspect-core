package org.usf.inspect.http;

import static java.time.Instant.now;
import static java.util.Map.entry;
import static java.util.Objects.nonNull;

import java.time.Instant;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

/**
 * 
 * @author u$f
 *
 */
final class HttpRequestMonitor extends AbstractHttpRequestMonitor {
	
	public void preExchange(HttpRequest request) { //no pre-process 
		var start = now();
		super.preExchange(request.getMethod(), request.getURI(), request.getHeaders())
			.fire(start, start, null, null);
	}

	public ExecutionListener<ClientHttpResponse> clientHttpResponseHandler() {
		return (s,e,res,t)-> {
			if(nonNull(res)) {
				super.postExchange().handle(s, e, entry(res.getStatusCode(), res.getHeaders()), t);
			}
			if(nonNull(t)) {
				super.disconnection().handle(s, e, null, null);
			}
		};
	}
	
	ExecutionListener<ResponseContent> postResponse(Instant start, Instant end, ResponseContent cnt, Throwable thrw) {
		return (s,e,o,t)-> {
			super.postResponse().fire(start, end, cnt, thrw);
			super.disconnection().handle(s, e, null, null);
		};
	}
}
