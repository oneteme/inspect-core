package org.usf.inspect.http;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.client.ClientHttpResponse;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class ClientHttpResponseWrapper implements ClientHttpResponse {

	@Delegate
	private final ClientHttpResponse response;
	private final HttpConnectionLifecycleTracer tracer;
	private InputStreamCaptor pipe;

	@Override
	public InputStream getBody() throws IOException {
		if(isNull(pipe)) {
			pipe = new InputStreamCaptor(response.getBody(), tracer, getStatusCode().isError());
		}
		return pipe;
	}
	
	@Override
	public void close() {
		try {
			response.close();
		}
		finally {
			tracer.complete(pipe);
		}
	}
}
