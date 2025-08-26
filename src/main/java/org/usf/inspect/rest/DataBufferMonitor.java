package org.usf.inspect.rest;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static org.usf.inspect.core.InspectContext.context;

import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

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
final class DataBufferMonitor {
	
	private final ContentReadMonitor monitor;
	private final AtomicBoolean done = new AtomicBoolean(false);

	private byte[] bytes;
	private long size;
	private Instant start;
	private Throwable throwable;
	
	
	Flux<DataBuffer> handle(Flux<DataBuffer> flux, boolean readContent) {
		start = now();
		return flux.map(db-> {
			try {
				var nb = db.readableByteCount();
				if(readContent && isNull(bytes) && nb > 0 && nb < 10_000) { //10kB max & no previous bytes
					bytes = new byte[nb];
					db.read(bytes);
				    return new DefaultDataBufferFactory().wrap(bytes);
				}
			}
			catch (Exception e) {
				context().reportEventHandleError("DataBufferMonitor.handle", null, e);
			}
			return db; //maybe consumed
		})
		.doOnNext(db-> size+= db.readableByteCount())
    	.doOnError(e-> throwable = e)
    	.doOnCancel(()-> throwable = new CancellationException("cancelled"))
    	.doFinally(v-> { //called 2 times
    		if(done.compareAndSet(false, true)) {
    			monitor.handle(start, now(), size, bytes, throwable);
    		}
    	});
	}
}