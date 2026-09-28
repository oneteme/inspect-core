package org.usf.inspect.http;

import static org.usf.inspect.core.HttpAction.EXECUTION;
import static org.usf.inspect.core.HttpAction.TRANSMISSION;
import static org.usf.inspect.core.TraceHub.hub;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor
final class HttpConnectionLifecycleTracer extends AbstractHttpConnectionLifecycleTracer {
	
	public ExecutionListener<ClientHttpResponse> exchangeStageListener(HttpRequest request) {
		return connectionListener(stageBuilder(EXECUTION), 
				(trc, res)-> signal((HttpRequestSignal)trc, request.getMethod(), request.getURI(), request.getHeaders()));
	}
	
	public ExecutionListener<StreamCaptor> streamStageListener(ClientHttpResponse res){
		return disconnectionListener((s,e,cnt,t)-> {
			try {
				traceHeaders(res.getStatusCode(), res.getHeaders()); 
				traceResponseContent(cnt);
			}
			catch (Exception ex) {
				hub().reportError("HttpConnectionLifecycleTracer.streamStageListener", ex);
			}
			return createStage(TRANSMISSION, s, e);
		});
	}
}
