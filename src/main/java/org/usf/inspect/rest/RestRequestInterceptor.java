package org.usf.inspect.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.requestAppender;
import static org.usf.inspect.core.StageTracker.call;
import static org.usf.inspect.rest.RestSessionFilter.TRACE_HEADER;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.RestRequest;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class RestRequestInterceptor implements ClientHttpRequestInterceptor { //see WebClientInterceptor
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		return call(()-> new ClientHttpResponseWrapper(execution.execute(request, body)), (s,e,res,t)->{
			var req = new RestRequest();
			req.setStart(s);
			req.setEnd(e);
			req.setThreadName(threadName());
			req.setMethod(request.getMethod().name());
			req.setURI(request.getURI());
			req.setAuthScheme(extractAuthScheme(request.getHeaders().get(AUTHORIZATION)));
			req.setOutDataSize(nonNull(body) ? body.length : 0);
			req.setOutContentEncoding(request.getHeaders().getFirst(CONTENT_ENCODING)); 
			//setUser(decode AUTHORIZATION)
			if(nonNull(res)) {
				req.setStatus(res.getStatusCode().value());
				req.setContentType(ofNullable(res.getHeaders().getContentType()).map(Object::toString).orElse(null));
				req.setInContentEncoding(res.getHeaders().getFirst(CONTENT_ENCODING)); 
				req.setId(res.getHeaders().getFirst(TRACE_HEADER));
				res.doOnClose((len, err)->{ //wait for reading content
					req.setInDataSize(len);
					if(nonNull(err)) { //4xx|5xx
						req.setException(new ExceptionInfo(null, new String(err, UTF_8)));
					}
				});
			}
			else if(nonNull(t)) { // IOException
				req.setException(mainCauseException(t));
			}
			return req;
		}, requestAppender());
	}
}

