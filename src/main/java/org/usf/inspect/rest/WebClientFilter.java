package org.usf.inspect.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.appendSessionStage;
import static org.usf.inspect.core.StageTracker.call;
import static org.usf.inspect.rest.RestSessionFilter.TRACE_HEADER;
import static reactor.core.publisher.Mono.just;

import java.time.Instant;
import java.util.List;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.RestRequest;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class WebClientFilter implements ExchangeFilterFunction {

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction exc) {
		var req = new RestRequest(); //see RestRequestInterceptor
		return call(()-> exc.exchange(request), (s,e,res,t)->{
			req.setStart(s);
			//async end
			req.setThreadName(threadName());
			req.setMethod(request.method().name());
			req.setProtocol(request.url().getScheme());
			req.setHost(request.url().getHost());
			req.setPort(request.url().getPort());
			req.setPath(request.url().getPath());
			req.setQuery(request.url().getQuery());
			req.setAuthScheme(extractAuthScheme(request.headers().get(AUTHORIZATION)));
			req.setOutDataSize(-2); //unknown !
			req.setOutContentEncoding(getFirstOrNull(request.headers().get(CONTENT_ENCODING))); 
			if(nonNull(t)) { //no response
				finalizeRequest(req, e, null, t);
			}
    		appendSessionStage(req);
		}).flatMap(res->{
			finalizeRequest(req, now(), res, null);
			return just(res.statusCode().isError() //4xx|5xx
					? res.mutate().body(f-> peekContentAsString(f, m-> req.setException(new ExceptionInfo(null, m)))).build()
					: res);
		}).doOnError(e-> finalizeRequest(req, now(), null, e)); //0
    }
    
    private void finalizeRequest(RestRequest req, Instant end, ClientResponse response, Throwable t) {
    	try { 
    		req.setEnd(end);
			if(nonNull(response)) {
				req.setStatus(response.statusCode().value());
				req.setContentType(response.headers().contentType().map(MediaType::getType).orElse(null));
				req.setInContentEncoding(getFirstOrNull(response.headers().header(CONTENT_ENCODING))); 
				req.setId(getFirstOrNull(response.headers().header(TRACE_HEADER))); //+ send api_name !?
				req.setInDataSize(-2); //unknown !
			}
			else if(nonNull(t)) {
				req.setException(mainCauseException(t));
			}
    	}
    	catch (Exception e) { //do not throw exception
    		log.warn("cannot collect request metrics, {}:{}", e.getClass().getSimpleName(), e.getMessage());
		}
    }
    
    static Flux<DataBuffer> peekContentAsString(Flux<DataBuffer> flux, Consumer<String> cons) {
    	return flux.map(db-> {
			try { //TD DataBuffer wrapper | pipe
				byte[] bytes = new byte[db.readableByteCount()];
				db.read(bytes);
			    cons.accept(new String(bytes, UTF_8));
			    return new DefaultDataBufferFactory().wrap(bytes);
			}
			catch (Exception e) {
				log.warn("cannot extract request body, {}:{}", e.getClass().getSimpleName(), e.getMessage());
				return db;
			}
		});
	}
    
    static <T> T getFirstOrNull(List<T> list) {
    	return isNull(list) || list.isEmpty() ? null : list.get(0);
    }
}
