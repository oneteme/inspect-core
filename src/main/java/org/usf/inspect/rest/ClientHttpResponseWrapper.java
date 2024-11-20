package org.usf.inspect.rest;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.InputStreamPipe.NO_CACHE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;

import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.InputStreamPipe;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;

@Setter
@RequiredArgsConstructor
public final class ClientHttpResponseWrapper implements ClientHttpResponse {

	@Delegate
	private final ClientHttpResponse cr;
	private BiConsumer<byte[], Integer> onClose;
	private InputStreamPipe pipe;
	
	@Override
	public InputStream getBody() throws IOException {
		return pipe = new InputStreamPipe(cr.getBody(), getStatusCode().isError());
	}
	
	@Override
	public void close() {
		try {
			if(nonNull(onClose) && nonNull(pipe)) {
				onClose.accept(pipe.getData(), pipe.getLength());
			}
		}
		finally {
			cr.close();
		}
	}
}
