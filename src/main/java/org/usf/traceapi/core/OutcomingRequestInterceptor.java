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
				var or = new OutcomingRequest(idProvider.get());
				or.setMethod(request.getMethodValue());
				or.setProtocol(request.getURI().getScheme());
				or.setHost(request.getURI().getHost());
				or.setPort(request.getURI().getPort());
				or.setPath(request.getURI().getPath());
				or.setQuery(request.getURI().getQuery());
				or.setStatus(res == null ? null : res.getRawStatusCode());
				or.setSize(request.getHeaders().getContentLength());
				or.setStart(ofEpochMilli(beg));
				or.setEnd(ofEpochMilli(fin));
				trc.append(or);
			}
		}
		return res;
	}
}