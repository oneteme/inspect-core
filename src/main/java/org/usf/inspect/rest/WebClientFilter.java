package org.usf.inspect.rest;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.springframework.web.reactive.function.client.ClientRequest.from;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.rest.FilterExecutionMonitor.TRACE_HEADER;
import static org.usf.inspect.rest.RestResponseMonitor.afterResponse;
import static org.usf.inspect.rest.RestResponseMonitor.emitRestRequest;
import static org.usf.inspect.rest.RestResponseMonitor.responseContentReadListener;

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
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction exc) {//request.headers is ReadOnlyHttpHeaders
		var req = emitRestRequest(request.method(), request.url(), request.headers());
		return call(()-> exc.exchange(from(request).header(TRACE_HEADER, req.getId()).build()), preRequestListener(req))
				.map(res->{
					var mnt = new DataBufferMonitor(responseContentReadListener(req));
					return res.mutate().body(f-> mnt.handle(f, res.statusCode().isError())).build();
				})
				.doOnNext(r-> traceHttpResponse(r, req, null))
				.doOnError(e-> traceHttpResponse(null, req, e)) //DnsNameResolverTimeoutException 
				.doOnCancel(()-> traceHttpResponse(null, req, new CancellationException("cancelled")));
	}

	ExecutionMonitorListener<Mono<ClientResponse>> preRequestListener(RestRequest req) {
		return (s,e,m,t)->{
			context().emitTrace(req.createStage(PRE_PROCESS, s, e, t));
			if(nonNull(t)) {
				req.runSynchronized(()-> req.setEnd(e));
				return req;
			}
			req.getAttributes().put(STAGE_START, e);
			return null; //request already traced
		};
	}

	private void traceHttpResponse(ClientResponse response, RestRequest req, Throwable thrw) {
		var now = now();
		var beg = (Instant) req.attribute(STAGE_START).orElse(now);
		if(nonNull(response)) {
			afterResponse(req, beg, now, response.statusCode().value(), response.headers().asHttpHeaders(), thrw);
		}
		else {
			afterResponse(req, beg, now, 0, null, thrw);
		}
	}
}
