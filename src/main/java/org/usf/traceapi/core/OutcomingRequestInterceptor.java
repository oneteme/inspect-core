package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
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
		var trc = localTrace.get();
		if(isNull(trc)) {
			return execution.execute(request, body);
		}
		ClientHttpResponse res = null;
		var out = new OutcomingRequest(idProvider.get());
		request.getHeaders().add(TRACE_HEADER, out.getId());
		var beg = currentTimeMillis();
		try {
			res = execution.execute(request, body);
		}
		finally {
			var fin = currentTimeMillis();
			out.setMethod(request.getMethodValue());
			out.setProtocol(request.getURI().getScheme());
			out.setHost(request.getURI().getHost());
			out.setPort(request.getURI().getPort());
			out.setPath(request.getURI().getPath());
			out.setQuery(request.getURI().getQuery());
			out.setStatus(res == null ? null : res.getRawStatusCode());
			out.setSize(request.getHeaders().getContentLength());
			out.setStart(ofEpochMilli(beg));
			out.setEnd(ofEpochMilli(fin));
			out.setThread(currentThread().getName());
			trc.append(out);
		}
		return res;
	}
}