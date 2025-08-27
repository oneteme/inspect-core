package org.usf.inspect.rest;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorReporter.reportError;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.CacheableInputStream;

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
	private final ContentReadMonitor monitor;
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
				if(nonNull(monitor)) { //unread body 
					if(nonNull(pipe)) {
						monitor.handle(start, end, pipe.getDataLength(), pipe.getData(), null); //data can be null if status=2xx
					}
					else { //start = end
						monitor.handle(end, end, -1, null, null);
					}
				}
			}
			catch (Exception e) {
				reportError("ClientHttpResponseWrapper.close", null, e);
			}
		}
	}
}
