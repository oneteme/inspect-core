package org.usf.inspect.http;

import static java.util.Objects.nonNull;
import static org.springframework.web.reactive.function.client.ClientRequest.from;
import static org.usf.inspect.core.InspectExecutor.call;
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
public final class WebClientFilter implements ExchangeFilterFunction { //see RestRequestInterceptor
	
	private static final int WAITING = 0;
    private static final int STREAMING = 1;
    private static final int COMPLETED = -1;

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction exc) {//request.headers is ReadOnlyHttpHeaders
		var trc = new AsyncHttpConnectionLifecycleTracer();
		var stt = new AtomicInteger(WAITING);
		return call(()-> exc.exchange(from(request).header(TRACE_ID_HEADER, trc.getId().toString()).build()), trc.assemblyStageListener(request))
				.map(res->{
					var buff = new DataBufferCaptor(trc);
					return res.mutate().body(f-> buff.handle(f, res.statusCode().isError())
							.doOnSubscribe(s-> stt.compareAndSet(WAITING, STREAMING))
							.doFinally(s-> complete(trc, buff, stt)))
							.build();
				})
				.doOnNext(r-> exchange(trc, r, null, stt))
				.doOnError(e-> exchange(trc, null, e, stt)) //DnsNameResolverTimeoutException 
				.doOnCancel(()-> exchange(trc, null, new CancellationException("cancelled"), stt))
				.doFinally(s-> complete(trc, null, stt));
	}
	
	static void exchange(AsyncHttpConnectionLifecycleTracer trc, ClientResponse res, Throwable thrw, AtomicInteger stt) {
		if(stt.get() != COMPLETED) {
			trc.exchangeStage(res, thrw);
		}
	}
	
	static void complete(AsyncHttpConnectionLifecycleTracer trc, StreamCaptor payload, AtomicInteger stt) {
		var act = nonNull(payload) ? STREAMING : WAITING;
		if(stt.compareAndSet(act, COMPLETED)) {
			trc.complete(payload); 
		}
	}
}
