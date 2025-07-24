package org.usf.inspect.rest;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.rest.RestResponseMonitorListener.afterResponse;
import static org.usf.inspect.rest.RestResponseMonitorListener.emitRestRequest;
import static org.usf.inspect.rest.RestResponseMonitorListener.responseContentReadListener;

import java.time.Instant;
import java.util.concurrent.CancellationException;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.RestRequest;

import reactor.core.publisher.Mono;

/**
 * 
 * @author u$f
 *
 */
public final class WebClientFilter implements ExchangeFilterFunction { //see RestRequestInterceptor

	private static final String STAGE_START = WebClientFilter.class.getName() + ".stageStart";
	
	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction exc) {
		var req = emitRestRequest(request.method(), request.url(), request.headers());
		return call(()-> exc.exchange(request), preRequestListener(request, req))
		.map(res->{
			var trck = new DataBufferMetricsTracker(responseContentReadListener(req));
			return res.mutate().body(f-> trck.track(f, res.statusCode().isError())).build();
		})
		.doOnNext(r-> traceHttpResponse(request, r, req, null))
		.doOnError(e-> traceHttpResponse(request, null, req, e)) //DnsNameResolverTimeoutException 
		.doOnCancel(()-> traceHttpResponse(request, null, req, new CancellationException("cancelled")));
    }
	
    ExecutionMonitorListener<Mono<ClientResponse>> preRequestListener(ClientRequest request, RestRequest req) {
    	return (s,e,m,t)->{
			context().emitTrace(req.createStage(PRE_PROCESS, s, e, t));
			req.runSynchronized(()->{
				req.setEnd(e);
				context().emitTrace(req);
			});
			request.attributes().put(STAGE_START, e);
		};
    }
    
    private void traceHttpResponse(ClientRequest request, ClientResponse response, RestRequest req, Throwable thrw) {
    	var now = now();
    	var beg = (Instant) request.attribute(STAGE_START).orElse(now);
    	if(nonNull(response)) {
        	afterResponse(req, beg, now, response.statusCode().value(), response.headers().asHttpHeaders(), thrw);
		}
		else {
	    	afterResponse(req, beg, now, 0, null, thrw);
		}
    }
}
