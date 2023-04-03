package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static org.usf.traceapi.core.ApiTraceFilter.TRACE_HEADER;
import static org.usf.traceapi.core.ApiTraceFilter.localTrace;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public final class ApiTraceInjector implements ClientHttpRequestInterceptor {
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		var trace = localTrace.get();
		if(trace != null) {
			var beg = currentTimeMillis();
			request.getHeaders().add(TRACE_HEADER, trace.getUuid());
			ClientHttpResponse res = null;
			try {
				res = execution.execute(request, body);
			}
			finally {
				var fin = currentTimeMillis();
				var stt = res == null ? null : res.getRawStatusCode();
				trace.getRequests().add(new SubRequest(request.getURI().toString(), request.getMethodValue(), beg, fin, stt));
			}
		}
		return execution.execute(request, body);
	}
}
