package org.usf.inspect.http;

import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

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
		var mnt = new HttpRequestMonitor();
		mnt.preExchange(request);
		request.getHeaders().set(TRACE_HEADER, mnt.getId());
		var res = call(()-> execution.execute(request, body), mnt.clientHttpResponseHandler());
		return new ClientHttpResponseWrapper(res, mnt.postResponse());
	}
}