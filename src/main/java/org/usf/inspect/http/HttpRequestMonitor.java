package org.usf.inspect.http;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.HttpAction.EXCHANGE;
import static org.usf.inspect.core.HttpAction.STREAM;
import static org.usf.inspect.core.SessionContextManager.createHttpRequest;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

/**
 * 
 * @author u$f
 *
 */
final class HttpRequestMonitor extends AbstractHttpRequestMonitor {

	ExecutionListener<ClientHttpResponse> exchangeHandler(HttpRequest request) {
		return traceBegin(t->
			createHttpRequest(t, getId()), 
			(req,o)-> fillRequest(req, request.getMethod(), request.getURI(), request.getHeaders()),
			traceStep((s,e,res,t)-> {
				if(nonNull(res)) {
					try {//execute postExchange after reading response 
						postExchange(res.getStatusCode(), res.getHeaders()); 
					}
					catch (Exception ex) {
						hub().reportError(true, "HttpRequestMonitor.exchangeHandler", ex);
					}
				}
				return createStage(EXCHANGE, s, e, t);
			}));
	}
	
	ExecutionListener<ResponseContent> responseHandler(){
		return traceEnd(traceStep((s,e,cnt,t)-> {
			try {
				postResponse(cnt);
			}
			catch (Exception ex) {
				hub().reportError(true, "HttpRequestMonitor.responseHandler", ex);
			}
			return createStage(STREAM, s, e, t);
		}));
	}
}
