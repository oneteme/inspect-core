package org.usf.inspect.http;

import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import lombok.RequiredArgsConstructor;

/**
 * Intercepts synchronous client HTTP requests and attaches tracing metadata.
 */
@RequiredArgsConstructor
public final class HttpRequestInterceptor implements ClientHttpRequestInterceptor { //see WebClientFilter
	
	/**
	 * Intercepts an outgoing request, adds the trace header, and wraps the response for monitoring.
	 *
	 * @param request the outgoing HTTP request
	 * @param body the serialized request body
	 * @param execution the request execution callback
	 * @return the monitored client HTTP response
	 * @throws IOException if request execution fails
	 */
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		var mnt = new HttpRequestMonitor();
		request.getHeaders().set(TRACE_HEADER, mnt.getId());
		var res = call(()-> execution.execute(request, body), mnt.exchangeHandler(request));
		return new ClientHttpResponseWrapper(res, mnt.responseHandler());
	}
}