package org.usf.inspect.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionManager.createHttpRequest;
import static org.usf.inspect.rest.FilterExecutionMonitor.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.usf.inspect.core.Helper;
import org.usf.inspect.core.RestRequest;

interface RestResponseMonitorListener {
	
	void handle(Instant start, Instant end, long size, byte[] res, Throwable t);
	
	static RestRequest emitRestRequest(HttpMethod mth, URI url, HttpHeaders headers) {
		var req = createHttpRequest();
		try {
			req.setStart(now());
    		req.setThreadName(threadName());
			req.setMethod(mth.name());
			req.setURI(url);
			req.setAuthScheme(extractAuthScheme(headers.get(AUTHORIZATION)));
			req.setOutDataSize(headers.getContentLength()); //-1 unknown !
			req.setOutContentEncoding(getFirstOrNull(headers.get(CONTENT_ENCODING))); 
			//req.setUser(decode AUTHORIZATION)
		} catch (Exception e) {
			context().reportEventHandleError(req.getId(), e);
		}
		headers.add(TRACE_HEADER, req.getId());
		context().emitTrace(req); //finally
		return req;
	}

	static void afterResponse(RestRequest req, Instant start, Instant end, int status, HttpHeaders headers, Throwable thrw) {
    		context().emitTrace(req.createStage(PROCESS, start, end, thrw)); //same thread
		req.runSynchronized(()->{
    		if(nonNull(headers)) { //response
        		req.setThreadName(threadName()); //deferred thread
    			req.setStatus(status);
    			req.setContentType(headers.getFirst(CONTENT_TYPE));
    			req.setInContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
    		}
    		if(nonNull(thrw)) {
    			req.setEnd(end);
    			context().emitTrace(req);
    		}
    	});
		if(nonNull(headers)) {
			req.assertRemoteID(headers.getFirst(TRACE_HEADER));
		}
	}
	
	static RestResponseMonitorListener responseContentReadListener(RestRequest req){
		return (s,e,n,b,t)-> {
			context().emitTrace(req.createStage(POST_PROCESS, s, e, t)); 
			req.runSynchronized(()->{
				if(nonNull(b)) {
					req.setBodyContent(new String(b, UTF_8));
				}
				req.setInDataSize(n);
				req.setEnd(e);
			});
			context().emitTrace(req);
		};
	}
    
    static <T> T getFirstOrNull(List<T> list) {
    	return isNull(list) || list.isEmpty() ? null : list.get(0);
    }

	
	public static String extractAuthScheme(List<String> authHeaders) { //nullable
		return nonNull(authHeaders) && authHeaders.size() == 1 //require one header
				? Helper.extractAuthScheme(authHeaders.get(0)) : null;
	}
}