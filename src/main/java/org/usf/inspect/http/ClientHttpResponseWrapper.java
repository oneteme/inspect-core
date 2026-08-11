package org.usf.inspect.http;

import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * Wraps a {@link ClientHttpResponse} to capture streamed response content.
 */
@Slf4j
@RequiredArgsConstructor
public final class ClientHttpResponseWrapper implements ClientHttpResponse {

	@Delegate
	private final ClientHttpResponse cr;
	private final ExecutionListener<ResponseContent> listener;
	private CacheableInputStream pipe;
	private Instant start = systemUTC().instant();

	/**
	 * Returns the response body stream, creating a cacheable wrapper on first access.
	 *
	 * @return the response body input stream
	 * @throws IOException if the response body cannot be obtained
	 */
	@Override
	public InputStream getBody() throws IOException {
		if(isNull(pipe)) {
			pipe = new CacheableInputStream(cr.getBody(), getStatusCode().isError());
		}
		return pipe;
	}
	
	/**
	 * Closes the wrapped response and publishes the captured response content metadata.
	 */
	@Override
	public void close() {
		Throwable t = null;
		try {
			cr.close();
		}
		catch (Exception e) {
			t = e;
			throw e;
		}
		finally {
			listener.safeHandle(start, systemUTC().instant(), pipe, t);
		}
	}
}
