package org.usf.inspect.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.util.FileCopyUtils.copyToByteArray;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.appendSessionStage;
import static org.usf.inspect.core.StageTracker.call;
import static org.usf.inspect.rest.RestSessionFilter.TRACE_HEADER;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
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
public final class RestRequestInterceptor implements ClientHttpRequestInterceptor {
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		return call(()-> execution.execute(request, body), (s,e,res,t)->{
			var req = new RestRequest(); //see WebClientInterceptor
			req.setMethod(request.getMethod().name());
			req.setProtocol(request.getURI().getScheme());
			req.setHost(request.getURI().getHost());
			req.setPort(request.getURI().getPort());
			req.setPath(request.getURI().getPath());
			req.setQuery(request.getURI().getQuery());
			req.setAuthScheme(extractAuthScheme(request.getHeaders().get(AUTHORIZATION)));
			req.setStart(s);
			req.setEnd(e);
			req.setOutDataSize(nonNull(body) ? body.length : -1);
			req.setOutContentEncoding(request.getHeaders().getFirst(CONTENT_ENCODING)); 
			req.setThreadName(threadName());
			//setUser(decode AUTHORIZATION)
			if(nonNull(res)) {
				req.setStatus(res.getStatusCode().value());
				req.setInDataSize(res.getBody().available()); //estimated !
				req.setContentType(ofNullable(res.getHeaders().getContentType()).map(MediaType::getType).orElse(null));
				req.setInContentEncoding(res.getHeaders().getFirst(CONTENT_ENCODING)); 
				req.setId(res.getHeaders().getFirst(TRACE_HEADER));
				if(res.getStatusCode().isError()) {
					req.setException(new ExceptionInfo(null, getResponseBody(res)));
				}
			}
			else if(nonNull(t)) { // IOException
				req.setException(mainCauseException(t));
			}
			appendSessionStage(req);
		});
	}
	
	static String getResponseBody(ClientHttpResponse response) {
        try {
            return new String(copyToByteArray(response.getBody()), UTF_8); // body is never null
        } catch (IOException e) { //do not throw exception
    		log.warn("cannot extract request body, {}:{}", e.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }
}

