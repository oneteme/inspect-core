package org.usf.inspect.http;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.HttpAction.EXCHANGE;
import static org.usf.inspect.core.HttpAction.STREAM;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionContextManager.createHttpRequest;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;

/**
 * 
 * @author u$f
 *
 */
final class HttpRequestMonitor extends AbstractHttpRequestMonitor {
	
	HttpRequestMonitor(HttpRequest request) {
		var now = now();
		traceBegin(t-> createHttpRequest(t, getId()), (req,o)-> 
		fillRequest(req, request.getMethod(), request.getURI(), request.getHeaders()))
		.safeHandle(now, now, null, null);
	}

	ExecutionListener<ClientHttpResponse> exchangeHandler() {
		ExecutionListener<ClientHttpResponse> lstn = traceStep((s,e,r,t)-> createStage(EXCHANGE, s, e, t));
		return lstn.then((s,e,res,t)->{
			if(nonNull(t)) {
				traceEnd().handle(s, e, null, t); //close if error
			}
		});
	}
	
	ExecutionListener<ResponseContent> responseHandler(ClientHttpResponse res){
		ExecutionListener<ResponseContent> lstn = traceStep((s,e,cnt,t)-> {
			if(nonNull(res)) {
				try {//execute postExchange after reading response 
					postExchange(res.getStatusCode(), res.getHeaders()); 
				}
				catch (Exception ex) {
					context().reportError(true, "HttpRequestMonitor.postExchange", ex);
				}
			}
			try {
				postResponse(cnt);
			}
			catch (Exception ex) {
				context().reportError(true, "HttpRequestMonitor.postResponse", ex);
			}
			return createStage(STREAM, s, e, t);
		});
		return lstn.then(traceEnd());
	}
}
