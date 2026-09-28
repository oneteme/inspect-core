package org.usf.inspect.http;

import static java.lang.Math.min;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.TraceHub.hub;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CancellationException;

import org.springframework.core.io.buffer.DataBuffer;

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
final class DataBufferCaptor implements StreamCaptor {

	private static final int MAX_BYTES_TO_CAPTURE = 4096; //4ko

	private final StreamExchangeListener listener;

	private ByteArrayOutputStream bufferStream;
	private byte[] bytes;
	private long size;
	private Throwable throwable;

	Flux<DataBuffer> handle(Flux<DataBuffer> flux, boolean readContent) {
		if(readContent) {
			bufferStream = new ByteArrayOutputStream(MAX_BYTES_TO_CAPTURE);
		}
		return flux.doOnSubscribe(s-> listener.onTransmissionStart())
		.doOnNext(this::onBuffer)
		.doOnError(e-> throwable = e)
		.doOnCancel(()-> throwable = new CancellationException("cancelled"))
		.doFinally(v-> {
			if(nonNull(bufferStream) && isNull(bytes)) {
				bytes = bufferStream.toByteArray();
			}
			listener.onTransmissionEnd();
		});
	}
	
	void onBuffer(DataBuffer db) {
		var readable = db.readableByteCount();
		size += readable;
		if(nonNull(bufferStream) && readable > 0) {
			var remaining = MAX_BYTES_TO_CAPTURE - bufferStream.size();
			if(remaining > 0) {
				try {
					int remainingToRead = min(readable, remaining);
					for(var it=db.readableByteBuffers(); it.hasNext() && remainingToRead>0;) {
						var slice = it.next().duplicate(); //duplicate to avoid changing the position of the original buffer
						int count = min(slice.remaining(), remainingToRead);
						if (count > 0) {
							var chunk = new byte[count];
							slice.get(chunk);
							bufferStream.write(chunk, 0, count);
							remainingToRead -= count;
						}
						else {
							break;
						}
					}
				} catch (Exception e) {
					hub().reportError("DataBufferCaptor.onBuffer", e);
					this.bufferStream = null; // stop capturing, keep counting
				}
			}
		}
	}

	@Override
	public byte[] bytes() {
		return bytes;
	}

	@Override
	public long size() {
		return size;
	}
	
	@Override
	public Throwable throwable() {
		return throwable;
	}
}