package org.usf.inspect.http;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static org.usf.inspect.core.ExecutionMonitor.trigger;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;

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
	private final ClientHttpResponse cr;
	private final ExecutionHandler<ResponseContent> monitor;
	private CacheableInputStream pipe;
	private Instant start = now();

	@Override
	public InputStream getBody() throws IOException {
		if(isNull(pipe)) {
			pipe = new CacheableInputStream(cr.getBody(), getStatusCode().isError());
		}
		return pipe;
	}
	
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
			trigger(monitor, start, now(), pipe, t);
		}
	}
}
