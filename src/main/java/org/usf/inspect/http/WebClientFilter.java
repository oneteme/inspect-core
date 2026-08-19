package org.usf.inspect.http;

import static org.springframework.web.reactive.function.client.ClientRequest.from;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

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

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction exc) {//request.headers is ReadOnlyHttpHeaders
		var mnt = new HttpRequestAsyncMonitor();
		var sync = new AtomicInteger(1);
		return call(()-> exc.exchange(from(request).header(TRACE_HEADER, mnt.getId()).build()), mnt.preExchange(request))
				.map(res->{
					sync.incrementAndGet();
					var buff = new DataBufferMonitor((s,e,ctn,t)->{
						if(sync.get() > 1) {
							mnt.postResponse(s, e, ctn, t);
						}
						if(sync.decrementAndGet() == 0) {
							mnt.complete(); //sometimes buffering ends after exchange.doFinally
						}
					});
					return res.mutate().body(f-> buff.handle(f, res.statusCode().isError())).build();
				})
				.doOnNext(r-> postExchange(mnt, r, null, sync))
				.doOnError(e-> postExchange(mnt, null, e, sync)) //DnsNameResolverTimeoutException 
				.doOnCancel(()-> postExchange(mnt, null, new CancellationException("cancelled"), sync))
				.doFinally(s-> { //called twice on cancel ?
					if(sync.decrementAndGet() == 0) {
						mnt.complete(); 
					}
				});
	}
	
	void postExchange(HttpRequestAsyncMonitor mnt, ClientResponse res, Throwable thrw, AtomicInteger sync) {
		if(sync.get() > 0) {
			mnt.postExchange(res, thrw);
		}
	}
}
