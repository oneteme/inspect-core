package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static org.usf.traceapi.core.IncomingRequestFilter.TRACE_HEADER;
import static org.usf.traceapi.core.TraceConfiguration.idProvider;
import static org.usf.traceapi.core.TraceConfiguration.localTrace;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 
 * @author u$f
 *
 */
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
				var siz = body == null ? -1 : body.length;
				trc.append(new OutcomingRequest(idProvider.get(), request.getURI().toString(), request.getMethodValue(), stt, siz, ofEpochMilli(beg), ofEpochMilli(fin)));
			}
		}
		return res;
	}
}