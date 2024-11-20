package org.usf.inspect.rest;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;

import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.CacheableInputStream;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class ClientHttpResponseWrapper implements ClientHttpResponse {

	@Delegate
	private final ClientHttpResponse cr;
	private BiConsumer<Integer, byte[]> onClose;
	private CacheableInputStream pipe;
	
	public void doOnClose(BiConsumer<Integer, byte[]> onClose) {
		this.onClose = onClose;
	}
	
	@Override
	public InputStream getBody() throws IOException {
		if(isNull(pipe)) {
			pipe = new CacheableInputStream(cr.getBody(), getStatusCode().isError());
		}
		return pipe;
	}
	
	@Override
	public void close() {
		try {
			if(nonNull(onClose) && nonNull(pipe)) {
				onClose.accept(pipe.getDataLength(), pipe.getData()); //data can be null if status=2xx
			}
		}
		finally {
			cr.close();
		}
	}
}
