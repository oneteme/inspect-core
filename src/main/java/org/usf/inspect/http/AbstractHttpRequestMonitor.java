package org.usf.inspect.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.Monitor.traceBegin;
import static org.usf.inspect.core.Monitor.traceStep;
import static org.usf.inspect.core.Monitor.traceEnd;
import static org.usf.inspect.core.SessionContextManager.createHttpRequest;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.util.Map.Entry;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.HttpRequest2;
import org.usf.inspect.core.HttpRequestCallback;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
class AbstractHttpRequestMonitor {

	@Getter private final String id = nextId();
	
	private HttpRequestCallback callback;

	ExecutionListener<Void> preExchange(HttpMethod method, URI uri, HttpHeaders headers) {
		return traceBegin(t-> createHttpRequest(t, id), this::createCallback, (req,o)->{
			if(nonNull(method)) {
				req.setMethod(method.name());
			}
			if(nonNull(uri)) {
				req.setURI(uri);
			}
			if(nonNull(headers)) {
				req.setAuthScheme(extractAuthScheme(headers.getFirst(AUTHORIZATION)));
				req.setDataSize(headers.getContentLength()); //-1 unknown !
				req.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
				//req.setUser(decode AUTHORIZATION)
			}
		}, (s,e,o,t)-> callback.createStage(PRE_PROCESS, s, e, t)); //before end if thrw
	}
	
	//callback should be created before processing
	HttpRequestCallback createCallback(HttpRequest2 session) { 
		return callback = session.createCallback();
	}
	
	ExecutionListener<Entry<HttpStatusCode, HttpHeaders>> postExchange() {
//		request.setThreadName(threadName()); //deferred thread
		return traceStep(callback, (s,e,entry,t)->{
			if(nonNull(entry)) {
				var status = entry.getKey();
				if(nonNull(status)) {
					callback.setStatus(status.value());
				}
				var headers = entry.getValue();
				if(nonNull(headers)) { //response
					callback.setContentType(headers.getFirst(CONTENT_TYPE));
					callback.setContentEncoding(headers.getFirst(CONTENT_ENCODING)); 
					callback.setLinked(assertSameID(headers.getFirst(TRACE_HEADER)));
				}
			}
			return callback.createStage(PROCESS, s, e, t);
		});
	}
	
	ExecutionListener<ResponseContent> postResponse(){
//		request.setThreadName(threadName()); //deferred thread
		return traceStep(callback, (s,e,cnt,t)->{
			if(nonNull(cnt)) {
				callback.setDataSize(cnt.contentSize());
				if(nonNull(cnt.contentBytes())) {
					callback.setBodyContent(new String(cnt.contentBytes(), UTF_8));
				}
			}
			else {
				callback.setDataSize(-1);
			}
			return callback.createStage(POST_PROCESS, s, e, t);
		});
	}
	
	ExecutionListener<Object> disconnection() {
		var call = callback;
		callback = null; //avoid reuse
		return traceEnd(call, null);
	}

	boolean assertSameID(String sid) {
		if(nonNull(sid)) {
			if(sid.equals(callback.getId())) {
				return true;
			}
			context().reportMessage(false, "assertSameID", "session.id=" + sid);
		}
		return false;
	}
}
