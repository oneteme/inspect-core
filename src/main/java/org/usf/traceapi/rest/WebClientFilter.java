package org.usf.traceapi.rest;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.extractAuthScheme;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.rest.RestSessionFilter.TRACE_HEADER;

import java.time.Instant;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.usf.traceapi.core.RestRequest;
import org.usf.traceapi.core.Session;

import reactor.core.publisher.Mono;

/**
 * 
 * @author u$f
 *
 */
public final class WebClientFilter implements ExchangeFilterFunction  {

	@Override
	public Mono<ClientResponse> filter(ClientRequest req, ExchangeFunction exc) {
    	var session = localTrace.get();
    	var start = now();
    	var res = exc.exchange(req);
		return isNull(session) ? res : res.doOnEach(s->{
    		if(s.isOnNext() || s.isOnError()) {
    			appnedRequest(session, start, now(), req, s.get(), s.getThrowable());
    		}
    	});
    }
    
    private void appnedRequest(Session session, Instant start, Instant end, ClientRequest request, ClientResponse response, Throwable th) {
    	try { 
    		var req = new RestRequest(); //see RestRequestInterceptor
			req.setMethod(request.method().name());
			req.setProtocol(request.url().getScheme());
			req.setHost(request.url().getHost());
			req.setPort(request.url().getPort());
			req.setPath(request.url().getPath());
			req.setQuery(request.url().getQuery());
			req.setAuthScheme(extractAuthScheme(request.headers().get(AUTHORIZATION)));
			req.setStart(start);
			req.setEnd(end);
			req.setOutDataSize(-2); //unknown !
			req.setOutContentEncoding(getFirstOrNull(request.headers().get(CONTENT_ENCODING))); 
			req.setException(mainCauseException(th));
			req.setThreadName(threadName());
			//setUser(decode AUTHORIZATION)
			if(nonNull(response)) {
				req.setStatus(response.statusCode().value());
				req.setInDataSize(-2); //unknown !
				req.setContentType(response.headers().contentType().map(MediaType::getType).orElse(null));
				req.setOutContentEncoding(getFirstOrNull(response.headers().header(CONTENT_ENCODING))); 
				req.setId(getFirstOrNull(response.headers().header(TRACE_HEADER))); //+ send api_name !?
			}
			session.append(req);
    	}
    	catch (Exception e) { //do not throw exception
    		log.warn("cannot collect request metrics, {}", e.getMessage());
		}
    }
    
    static <T> T getFirstOrNull(List<T> list) {
    	return isNull(list) || list.isEmpty() ? null : list.get(0);
    }
}
