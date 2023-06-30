package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.usf.traceapi.core.Helper.extractAuthScheme;
import static org.usf.traceapi.core.Helper.idProvider;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.IncomingRequestFilter.TRACE_HEADER;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class OutcomingRequestInterceptor implements ClientHttpRequestInterceptor {
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		ClientHttpResponse res = null;
		var out = new OutcomingRequest(idProvider.get());
		request.getHeaders().add(TRACE_HEADER, out.getId());
		var beg = currentTimeMillis();
		try {
			res = execution.execute(request, body);
		}
		finally {
			var fin = currentTimeMillis();
			try {
				out.setMethod(request.getMethod().name());
				out.setProtocol(request.getURI().getScheme());
				out.setHost(request.getURI().getHost());
				out.setPort(request.getURI().getPort());
				out.setPath(request.getURI().getPath());
				out.setQuery(request.getURI().getQuery());
				out.setAuthScheme(extractAuthScheme(request.getHeaders().get(AUTHORIZATION)));
				out.setStart(ofEpochMilli(beg));
				out.setEnd(ofEpochMilli(fin));
				out.setOutDataSize(nonNull(body) ? body.length : 0);
				out.setThreadName(threadName());
				if(nonNull(res)) {
					out.setStatus(res.getStatusCode().value());
					out.setInDataSize(res.getBody().available()); //not exact !?
					out.setContentType(ofNullable(res.getHeaders().getContentType()).map(MediaType::getType).orElse(null));
				}
				var trc = localTrace.get();
				if(isNull(trc)) { //no session
					//orphan
				}
				else {
					trc.append(out);
				}
			}
			catch(Exception e) {
				//do not catch exception
				log.warn("error while tracing : {}" + request, e);
			}
		}
		return res;
	}
}