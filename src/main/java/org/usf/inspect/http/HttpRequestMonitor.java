package org.usf.inspect.http;

import static java.time.Instant.now;
import static java.util.Map.entry;
import static java.util.Objects.nonNull;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

/**
 * 
 * @author u$f
 *
 */
final class HttpRequestMonitor extends AbstractHttpRequestMonitor {
	
	HttpRequestMonitor(HttpRequest request) {
		var start = now();
		super.preExchange(request.getMethod(), request.getURI(), request.getHeaders())
			.safeHandle(start, start, null, null);
	}

	ExecutionListener<ClientHttpResponse> clientHttpResponseHandler() {
		return (s,e,res,t)-> {
			if(nonNull(res)) {
				super.postExchange().handle(s, e, entry(res.getStatusCode(), res.getHeaders()), t);
			}
			if(nonNull(t)) {
				super.disconnection().handle(s, e, null, t);
			}
		};
	}
	
	@Override
	ExecutionListener<ResponseContent> postResponse() {
		return (s,e,o,t)-> {
			super.postResponse().safeHandle(s, e, o, t);
			super.disconnection().handle(s, e, null, null);
		};
	}
}
