package org.usf.traceapi.rest;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.extractAuthScheme;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.stackTraceElement;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.Helper.warnNoActiveSession;
import static org.usf.traceapi.core.StageTracker.call;
import static org.usf.traceapi.rest.RestSessionFilter.TRACE_HEADER;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.traceapi.core.RestRequest;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class RestRequestInterceptor implements ClientHttpRequestInterceptor {
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		var session = localTrace.get();
		if(isNull(session)) {
			warnNoActiveSession();
			return execution.execute(request, body);
		}
		return call(()-> execution.execute(request, body), (s,e,res,t)->{
			var out = new RestRequest();
			out.setMethod(request.getMethod().name());
			out.setProtocol(request.getURI().getScheme());
			out.setHost(request.getURI().getHost());
			out.setPort(request.getURI().getPort());
			out.setPath(request.getURI().getPath());
			out.setQuery(request.getURI().getQuery());
			out.setAuthScheme(extractAuthScheme(request.getHeaders().get(AUTHORIZATION)));
			out.setStart(s);
			out.setEnd(e);
			out.setOutDataSize(nonNull(body) ? body.length : -1);
			out.setOutContentEncoding(request.getHeaders().getFirst(CONTENT_ENCODING)); 
			out.setException(mainCauseException(t));
			out.setThreadName(threadName());
			stackTraceElement().ifPresent(st->{
				out.setName(st.getMethodName());
				out.setLocation(st.getClassName());
			});
//			setUser if auth=Basic !
			if(nonNull(res)) {
				out.setStatus(res.getStatusCode().value());
				out.setInDataSize(res.getBody().available()); //estimated !
				out.setContentType(ofNullable(res.getHeaders().getContentType()).map(MediaType::getType).orElse(null));
				out.setOutContentEncoding(res.getHeaders().getFirst(CONTENT_ENCODING)); 
				out.setId(res.getHeaders().getFirst(TRACE_HEADER)); //+ send api_name !?
			}
			session.append(out);
		});
	}
}