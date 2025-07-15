package org.usf.inspect.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.InspectContext.emit;
import static org.usf.inspect.core.SessionManager.createHttpRequest;
import static org.usf.inspect.rest.FilterExecutionMonitor.TRACE_HEADER;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CancellationException;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.RestRequest;
import org.usf.inspect.rest.RestRequestInterceptor.RestExecutionMonitorListener;

import lombok.extern.slf4j.Slf4j;
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
		var req = createHttpRequest(); //see RestRequestInterceptor
		return call(()-> exc.exchange(request), restRequestListener(req, request))
		.map(res->{
			var trck = new DataBufferMetricsTracker(contentReadListener(req));
			return res.mutate().body(f-> trck.track(f, res.statusCode().isError())).build();
		})
		.doOnNext(r-> traceHttpResponse(req, now(), r, null))
		.doOnError(e-> traceHttpResponse(req, now(), null, e)) //DnsNameResolverTimeoutException 
		.doOnCancel(()-> traceHttpResponse(req, now(), null, new CancellationException("cancelled")));
    }

    ExecutionMonitorListener<Mono<ClientResponse>> restRequestListener(RestRequest req, ClientRequest request) {
    	return (s,e,res,t)->{
			req.setStart(s);
			req.setMethod(request.method().name());
			req.setURI(request.url());
			req.setAuthScheme(extractAuthScheme(request.headers().get(AUTHORIZATION)));
			req.setOutDataSize(request.headers().getContentLength()); //-1 unknown !
			req.setOutContentEncoding(getFirstOrNull(request.headers().get(CONTENT_ENCODING))); 
			//req.setUser(decode AUTHORIZATION)
			if(nonNull(t)) { //no response
				traceHttpResponse(req, now(), null, t);
			}
			else {
				emit(req); //no action
			}
		};
    }

    private void traceHttpResponse(RestRequest req, Instant end, ClientResponse cr, Throwable thrw) {
    	var tn = threadName(); //run outside task
		var stts = nonNull(cr) ? cr.statusCode().value() : 0; //break ClientHttpRes. dependency
		var ctty = nonNull(cr) ? cr.headers().asHttpHeaders().getFirst(CONTENT_TYPE) : null;
		var cten = nonNull(cr) ? cr.headers().asHttpHeaders().getFirst(CONTENT_ENCODING) : null;
		var id   = nonNull(cr) ? cr.headers().asHttpHeaders().getFirst(TRACE_HEADER) : null;
		emit(req.createStage(PROCESS, req.getStart(), end, thrw)); //same thread
    	req.runSynchronized(()->{
    		req.setThreadName(tn);
			req.setId(id); //+ send api_name !?
			req.setStatus(stts);
			req.setContentType(ctty);
			req.setInContentEncoding(cten); 
    		if(nonNull(thrw)) {
    			req.setEnd(end);
    			emit(req);
    		}
    	});
    }
    
	RestExecutionMonitorListener contentReadListener(RestRequest req){
		return (s,e,n,b,t)-> {
			emit(req.createStage(POST_PROCESS, s, e, t)); //READ content
			req.runSynchronized(()->{
				if(nonNull(b)) {
					req.setBodyContent(new String(b, UTF_8));
				}
				req.setInDataSize(n);
				req.setEnd(e);
			});
			emit(req);
		};
	}
    
    static <T> T getFirstOrNull(List<T> list) {
    	return isNull(list) || list.isEmpty() ? null : list.get(0);
    }
}
