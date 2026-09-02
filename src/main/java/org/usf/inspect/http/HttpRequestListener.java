package org.usf.inspect.http;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.HttpAction.EXCHANGE;
import static org.usf.inspect.core.HttpAction.STREAM;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

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
final class HttpRequestListener extends AbstractHttpRequestListener {
	
	public ExecutionListener<ClientHttpResponse> exchangeStageListener(HttpRequest request) {
		return connectionListener(stageBuilder(EXCHANGE), (trc, res)->
			signal((HttpRequestSignal)trc, request.getMethod(), request.getURI(), request.getHeaders()));
	}
	
	public ExecutionListener<ResponseContent> streamStageListener(ClientHttpResponse res){
		if(nonNull(res)) {
			try {//execute postExchange after reading response 
				traceHeaders(res.getStatusCode(), res.getHeaders()); 
			}
			catch (Exception ex) {
				hub().reportError(true, "HttpRequestMonitor.responseHandler", ex);
			}
		}
		return disconnectionListener((s,e,cnt,t)-> {
			try {
				traceResponseContent(cnt);
			}
			catch (Exception ex) {
				hub().reportError(true, "HttpRequestMonitor.responseHandler", ex);
			}
			return createStage(STREAM, s, e);
		});
	}
}
