package org.usf.inspect.http;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.HttpAction.EXCHANGE;
import static org.usf.inspect.core.HttpAction.STREAM;

import java.time.Instant;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.TraceHub;

import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor
final class HttpRequestListener extends AbstractHttpRequestListener<HttpRequest> {
	
	public HttpRequestListener(TraceHub hub) {
		super(hub);
	}

	@Override
	protected HttpRequestSignal signal(Instant start, HttpRequest cnx) throws Exception {
		return signal(start, cnx.getMethod(), cnx.getURI(), cnx.getHeaders());
	}
	
	ExecutionListener<ClientHttpResponse> exchangeStageListener(HttpRequest request) {
		return connectionListener(stageBuilder(EXCHANGE), v-> request);
	}
	
	ExecutionListener<ResponseContent> streamStageListener(ClientHttpResponse res){
		if(nonNull(res)) {
			try {//execute postExchange after reading response 
				traceHeaders(res.getStatusCode(), res.getHeaders()); 
			}
			catch (Exception ex) {
				getHub().reportError(true, "HttpRequestMonitor.responseHandler", ex);
			}
		}
		return disconnectionListener((s,e,cnt,t)-> {
			try {
				traceResponseContent(cnt);
			}
			catch (Exception ex) {
				getHub().reportError(true, "HttpRequestMonitor.responseHandler", ex);
			}
			return createStage(STREAM, s, e);
		});
	}
}
