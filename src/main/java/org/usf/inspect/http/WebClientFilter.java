package org.usf.inspect.http;

import static java.util.Objects.nonNull;
import static org.springframework.web.reactive.function.client.ClientRequest.from;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.TraceHub.hub;
import static org.usf.inspect.http.WebUtils.TRACE_ID_HEADER;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import reactor.core.publisher.Mono;

/**
 * 
 * @author u$f
 *
 */
public final class WebClientFilter implements ExchangeFilterFunction {
	
	private static final int WAITING = 0;
    private static final int STREAMING = 1;
    private static final int COMPLETED = -1;

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction exc) {
		var trc = new AsyncHttpConnectionLifecycleTracer();
		var stt = new AtomicInteger(WAITING);
		return call(()-> exc.exchange(request(trc, request)), trc.assemblyStageListener(request))
				.map(res-> response(trc, res, stt))
				.doOnNext(r-> exchange(trc, r, null, stt))
				.doOnError(e-> exchange(trc, null, e, stt)) //DnsNameResolverTimeoutException 
				.doOnCancel(()-> exchange(trc, null, new CancellationException("cancelled"), stt));
//				.doFinally(s-> complete(trc, null, stt))
	}
	
	static ClientRequest request(AsyncHttpConnectionLifecycleTracer trc, ClientRequest request) {
		try {
			return from(request).header(TRACE_ID_HEADER, trc.getId().toString()).build();
		}
		catch (Exception e) {
			hub().reportError("WebClientFilter.request", e);
		}
		return request;
	}

	static ClientResponse response(AsyncHttpConnectionLifecycleTracer trc, ClientResponse reponse, AtomicInteger stt) {
		try {
			var cpt = new DataBufferCaptor(trc);
			return reponse.mutate().body(f-> cpt.handle(f, reponse.statusCode().isError())
					.doOnSubscribe(s-> stt.compareAndSet(WAITING, STREAMING))
					.doFinally(s-> complete(trc, cpt, stt)))
					.build();
		}
		catch (Exception e) {
			hub().reportError("WebClientFilter.response", e);
		}
		return reponse;
	}
	
	static void exchange(AsyncHttpConnectionLifecycleTracer trc, ClientResponse reponse, Throwable thrw, AtomicInteger stt) {
		if(stt.getAndUpdate(v-> nonNull(thrw) ? COMPLETED : v) != COMPLETED) {
			trc.exchangeStage(reponse, thrw);
			if(nonNull(thrw)) {
				trc.complete(null);
			}
		}
	}
	
	static void complete(AsyncHttpConnectionLifecycleTracer trc, StreamCaptor captor, AtomicInteger stt) {
		if(stt.getAndSet(COMPLETED) != COMPLETED) {
			trc.complete(captor); 
		}
	}
}
