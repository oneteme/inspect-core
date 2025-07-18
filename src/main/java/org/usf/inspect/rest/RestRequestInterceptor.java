package org.usf.inspect.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionManager.createHttpRequest;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;
import static org.usf.inspect.rest.FilterExecutionMonitor.TRACE_HEADER;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.RestRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class RestRequestInterceptor implements ClientHttpRequestInterceptor { //see WebClientInterceptor
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		var req = traceHttpRequest(request, body);
		var res = call(()-> execution.execute(request, body), httpResponseListener(req));
		return new ClientHttpResponseWrapper(res, contentReadListener(req));
	}
	
	RestRequest traceHttpRequest(HttpRequest request, byte[] body) {
		var req = createHttpRequest();
		try {
			req.setStart(now());
			req.setMethod(request.getMethod().name());
			req.setURI(request.getURI());
			req.setAuthScheme(extractAuthScheme(request.getHeaders().get(AUTHORIZATION)));
			req.setOutDataSize(nonNull(body) ? body.length : 0);
			req.setOutContentEncoding(request.getHeaders().getFirst(CONTENT_ENCODING)); 
			//req.setUser(decode AUTHORIZATION)
			context().emitTrace(req);
		} catch (Throwable e) {
			log.warn("cannot collect request metrics, {}:{}", e.getClass().getSimpleName(), e.getMessage());
		}
		return req;
	}

	ExecutionMonitorListener<ClientHttpResponse> httpResponseListener(RestRequest req) {
		var upd = requireCurrentSession();
		return (s,e,r,t)->{
			if(nonNull(upd)) {
				upd.updateContext(); // if parallel execution
			}
			var tn   = threadName(); //exec outside
			var id   = nonNull(r) ? r.getHeaders().getFirst(TRACE_HEADER) : null;
			var ctty = nonNull(r) ? r.getHeaders().getFirst(CONTENT_TYPE) : null;
			var cten = nonNull(r) ? r.getHeaders().getFirst(CONTENT_ENCODING) : null;
			var stts = nonNull(r) ? r.getStatusCode().value() : 0; //break ClientHttpRes. dependency
			context().emitTrace(req.createStage(PROCESS, s, e, t));
			req.runSynchronized(()-> {
				req.setThreadName(tn);
				req.setId(id);
				req.setStatus(stts);
				req.setContentType(ctty);
				req.setInContentEncoding(cten); 
				if(nonNull(t)) { // IOException
					req.setEnd(e);
					context().emitTrace(req);
				}
			});
		};
	}
	
	RestExecutionMonitorListener contentReadListener(RestRequest req){
		var upd = requireCurrentSession();
		return (s,e,n,b,t)-> {
			if(nonNull(upd)) {
				upd.updateContext(); // deferred execution
			}
			context().emitTrace(req.createStage(POST_PROCESS, s, e, t)); //red content
			req.runSynchronized(()-> {
				if(nonNull(b)) {
					req.setBodyContent(new String(b, UTF_8));
				}
				req.setInDataSize(n);
				req.setEnd(e);
			});
			context().emitTrace(req);
		};
	}
	
	static interface RestExecutionMonitorListener {
		
		void handle(Instant start, Instant end, long size, byte[] res, Throwable t);
	}
}

