package org.usf.inspect.http;

import static org.usf.inspect.core.HttpAction.EXECUTION;
import static org.usf.inspect.core.TraceHub.hub;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

/**
 * 
 * @author u$f
 *
 */
final class HttpConnectionLifecycleTracer extends AbstractHttpConnectionLifecycleTracer {
	
	public ExecutionListener<ClientHttpResponse> exchangeStageListener(HttpRequest request) {
		return connectionListener((s,e,o,t)-> {
			try {
				traceHeaders(o.getStatusCode(), o.getHeaders()); 
			}
			catch (Exception ex) {
				hub().reportError("HttpConnectionLifecycleTracer.exchangeStageListener", ex);
			}
			return createStage(EXECUTION, s, e); 
		},(trc, res)-> signal((HttpRequestSignal)trc, request.getMethod(), request.getURI(), request.getHeaders()));
	}
}
