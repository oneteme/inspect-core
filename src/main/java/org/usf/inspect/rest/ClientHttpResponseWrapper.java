package org.usf.inspect.rest;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.CacheableInputStream;
import org.usf.inspect.rest.RestRequestInterceptor.RestExecutionMonitorListener;

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
	private final RestExecutionMonitorListener handler;
	private CacheableInputStream pipe;
	private Instant start;
		
	@Override
	public InputStream getBody() throws IOException {
		if(isNull(pipe)) {
			start = now();
			pipe = new CacheableInputStream(cr.getBody(), getStatusCode().isError());
		}
		return pipe;
	}
	
	@Override
	public void close() {
		try {
			cr.close();
		}
		finally {
			var end = now();
			try {
				if(nonNull(handler)) { //unread body 
					if(nonNull(pipe)) {
						handler.handle(start, end, pipe.getDataLength(), pipe.getData(), null); //data can be null if status=2xx
					}
					else { //start = end
						handler.handle(end, end, -1, null, null);
					}
				}
			}
			catch (Exception e) {
				log.warn("Error while handling request response {}: {}", 
						e.getClass().getSimpleName(), e.getMessage());
			}
		}
	}
}
