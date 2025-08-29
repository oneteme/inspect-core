package org.usf.inspect.http;

import static org.springframework.web.reactive.function.client.ClientRequest.from;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.util.concurrent.CancellationException;

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
		return call(()-> exc.exchange(from(request).header(TRACE_HEADER, mnt.getRequest().getId()).build()), mnt.preProcessHandler(request))
				.map(res->{
					var buff = new DataBufferMonitor(mnt::completeHandler);
					return res.mutate().body(f-> buff.handle(f, res.statusCode().isError())).build();
				})
				.doOnNext(r-> mnt.postProcess(r, null))
				.doOnError(e-> mnt.postProcess(null, e)) //DnsNameResolverTimeoutException 
				.doOnCancel(()-> mnt.postProcess(null, new CancellationException("cancelled")));
	}	
}
