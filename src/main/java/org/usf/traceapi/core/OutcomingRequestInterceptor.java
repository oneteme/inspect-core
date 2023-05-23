package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static org.usf.traceapi.core.IncomingRequestFilter.TRACE_HEADER;
import static org.usf.traceapi.core.TraceConfiguration.idProvider;
import static org.usf.traceapi.core.TraceConfiguration.localTrace;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public final class OutcomingRequestInterceptor implements ClientHttpRequestInterceptor {
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		ClientHttpResponse res = null;
		var trc = localTrace.get();
		if(trc == null) {
			res = execution.execute(request, body);
		}
		else {
			request.getHeaders().add(TRACE_HEADER, trc.getId());
			var beg = currentTimeMillis();
			try {
				res = execution.execute(request, body);
			}
			finally {
				var fin = currentTimeMillis();
				var stt = res == null ? null : res.getRawStatusCode();
				trc.push(new OutcomingRequest(idProvider.get(), request.getURI().toString(), request.getMethodValue(), stt, beg, fin));
			}
		}
		return res;
	}
}