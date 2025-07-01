package org.usf.inspect.rest;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static reactor.core.publisher.Flux.defer;

import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.usf.inspect.rest.RestRequestInterceptor.RestExecutionMonitorListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
final class DataBufferMetricsTracker {
	
	private final RestExecutionMonitorListener listener;

	private byte[] bytes;
	private long size;
	private Instant start;
	private Throwable throwable;
	
	Flux<DataBuffer> track(Flux<DataBuffer> flux, boolean readContent) {
		return defer(() -> { //multiple subscribers
    		var flag = new AtomicBoolean(false); 
	    	return flux.map(db-> {
	    		flag.set(true); // real subscriber
	    		start = now();
				try {
					var nb = db.readableByteCount();
					if(readContent && isNull(bytes) && nb > 0 && nb < 10_000) { //10kB max & no previous bytes
						bytes = new byte[nb];
						db.read(bytes);
					    return new DefaultDataBufferFactory().wrap(bytes);
					}
				}
				catch (Throwable e) {
					log.warn("cannot extract request body, {}:{}", e.getClass().getSimpleName(), e.getMessage());
				}
				return db; //maybe consumed
			})
			.doOnNext(db-> size+= db.readableByteCount())
	    	.doOnError(e-> throwable = e)
	    	.doOnCancel(()-> throwable = new CancellationException("cancelled"))
	    	.doFinally(v->{
	    		if(flag.get()) { //real subscriber
	    			listener.handle(start, now(), size, bytes, throwable);
	    		}
			});
		});
    }
}