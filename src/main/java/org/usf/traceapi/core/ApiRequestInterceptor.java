package org.usf.traceapi.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.usf.traceapi.core.ApiSessionFilter.TRACE_HEADER;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.extractAuthScheme;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.Helper.warnNoSession;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
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
public final class ApiRequestInterceptor implements ClientHttpRequestInterceptor {
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		var session = localTrace.get();
		if(isNull(session)) {
			warnNoSession();
			return execution.execute(request, body);
		}
		log.trace("outcoming request : {}", request.getURI());
		var out = new ApiRequest();
		ClientHttpResponse res = null; 
		Throwable ex = null;
		var beg = now();
		try {
			res = execution.execute(request, body);
		}
		catch(Exception e) {
			ex =  e;
			throw e;
		}
		finally {
			var fin = now();
			try {
				out.setMethod(request.getMethod().name());
				out.setProtocol(request.getURI().getScheme());
				out.setHost(request.getURI().getHost());
				out.setPort(request.getURI().getPort());
				out.setPath(request.getURI().getPath());
				out.setQuery(request.getURI().getQuery());
				out.setAuthScheme(extractAuthScheme(request.getHeaders().get(AUTHORIZATION)));
				out.setStart(beg);
				out.setEnd(fin);
				out.setOutDataSize(nonNull(body) ? body.length : 0);
				out.setException(mainCauseException(ex));
				out.setThreadName(threadName());
				if(nonNull(res)) {
					out.setStatus(res.getStatusCode().value());
					out.setInDataSize(res.getBody().available()); //not exact !?
					out.setContentType(ofNullable(res.getHeaders().getContentType()).map(MediaType::getType).orElse(null));
					out.setId(ofNullable(res.getHeaders().getFirst(TRACE_HEADER)).orElse(null)); //+ send api_name !?
//					setUser!
				}
				session.append(out);
			}
			catch(Exception e) {
				log.warn("error while tracing : " + request, e);
				//do not throw exception
			}
		}
		return res;
	}
}