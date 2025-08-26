package org.usf.inspect.rest;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.rest.FilterExecutionMonitor.TRACE_HEADER;
import static org.usf.inspect.rest.RestResponseMonitor.afterResponse;
import static org.usf.inspect.rest.RestResponseMonitor.emitRestRequest;
import static org.usf.inspect.rest.RestResponseMonitor.responseContentReadListener;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.RestRequest;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class RestRequestInterceptor implements ClientHttpRequestInterceptor { //see WebClientFilter
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		var req = emitRestRequest(request.getMethod(), request.getURI(), request.getHeaders());
		request.getHeaders().add(TRACE_HEADER, req.getId());
		var res = call(()-> execution.execute(request, body), httpResponseListener(req));
		return new ClientHttpResponseWrapper(res, responseContentReadListener(req));
	}

	ExecutionMonitorListener<ClientHttpResponse> httpResponseListener(RestRequest req) {
		return (s,e,r,t)->{
			if(nonNull(r)) {
				afterResponse(req, s, e, r.getStatusCode().value(), r.getHeaders(), t);
			}
			else {
				afterResponse(req, s, e, 0, null, t);
			}
			return null;
		};
	}
}