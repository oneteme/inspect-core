package org.usf.inspect.http;

import static java.net.URI.create;
import static java.time.Clock.systemUTC;
import static java.util.Arrays.stream;
import static java.util.Collections.list;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.function.Predicate.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.USER_AGENT;
import static org.usf.inspect.core.HttpAction.DELEGATION;
import static org.usf.inspect.core.HttpAction.EXECUTION;
import static org.usf.inspect.core.HttpAction.FINALIZATION;
import static org.usf.inspect.core.HttpAction.INITIALIZATION;
import static org.usf.inspect.core.HttpAction.STREAM;
import static org.usf.inspect.core.SessionContextManager.createHttpSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.TraceDispatcherHub.hub;
import static org.usf.inspect.http.WebUtils.TRACE_ID_HEADER;
import static org.usf.inspect.http.WebUtils.extractAuthScheme;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.usf.inspect.core.ExecutionTracer;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpSessionStage;
import org.usf.inspect.core.HttpSessionUpdate;
import org.usf.inspect.core.TraceUpdate;
import org.usf.inspect.http.TransferPayload.StreamPayload;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * Filter → Interceptor.preHandle → Deferred → Controller(task-?) →   Filter → Interceptor.preHandle → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
 * Filter → Interceptor.preHandle → Controller → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
 * 
 * @author u$f 
 *
 */
public final class HttpSessionTracer extends ExecutionTracer<Void> {
	
	private final AtomicInteger stageCounter = new AtomicInteger();
	@Getter
	private final StreamPayload streamPayload = new StreamPayload();
	
	private Throwable lastException;
	private Instant lastTimestamp;
	
	@Setter
	private HttpServletResponse response;

	public HttpSessionTracer(TraceUpdate update, Instant start) {
		super(update);
		this.lastTimestamp = start;
	}
	
	public static HttpSessionTracer httpSessionTracer(HttpServletRequest request) {
		var sgn = createHttpSession(systemUTC().instant(), parseUUID(request.getHeader(TRACE_ID_HEADER)));
		var signal = traceSignal(()->{
			sgn.setMethod(request.getMethod());
			sgn.setURI(fromRequest(request));
			sgn.setAuthScheme(extractAuthScheme(request.getHeader(AUTHORIZATION))); //extract user !?
			sgn.setDataSize(request.getContentLength());
			sgn.setContentEncoding(request.getHeader(CONTENT_ENCODING));
			sgn.setUserAgent(request.getHeader(USER_AGENT));
			sgn.setForwardedAddresses(extractAllHeaderValues(request, "X-Forwarded-For"));
			return sgn;
		});
		var upd = new HttpSessionUpdate(signal.getId());
		return new HttpSessionTracer(upd, signal.getStart());
	}
	
	@Override
	public HttpSessionUpdate getUpdate() {
		return (HttpSessionUpdate) super.getUpdate();
	}
	
	@Override
	public void handle(Instant start, Instant end, Void obj, Throwable thrw) throws Exception {
		if(assertActiveTraceUpdate("HttpSessionTracer.handle")) {
			var upd = getUpdate();
			if(nonNull(streamPayload.getStart())) {
				var stg = new HttpSessionStage(upd.getId(), stageCounter.incrementAndGet());
				stg.setName(STREAM.name());
				stg.setStart(streamPayload.getStart());
				stg.setEnd(nonNull(streamPayload.getEnd()) ? streamPayload.getEnd() : end);
				hub().emitTrace(stg);
			}
			if(nonNull(response)){
				upd.setStatus((short)response.getStatus());
				upd.setDataSize(streamPayload.getSize().get()); //response.getBufferSize()
				upd.setContentType(response.getContentType());
				upd.setContentEncoding(response.getHeader(CONTENT_ENCODING)); 
				upd.setCacheControl(response.getHeader(CACHE_CONTROL));
			}
			else {
				hub().reportMessage("HttpSessionTracer.handle", "response is null");
			}
			super.handle(start, end, null, thrw);
		}
	}
	
	public void propagateContext() {
		setActiveContext(getUpdate());
	}

	public void emitInitializationStage(String name, String user){
		emitStage(INITIALIZATION); //signal can be traced here
		var upd = getUpdate();
		upd.setName(name);
		upd.setUser(user);
	}
	
	public void emitDelegationStage() {
		emitStage(DELEGATION);
	}
	
	public void emitExecutionStage(){ //see this.asyncPostFilterHander
		emitStage(EXECUTION);
	}

	public void emitFinalizationStage(){
		emitStage(FINALIZATION);
	}

	void emitStage(HttpAction action) {
		var end = systemUTC().instant();
		var stg = new HttpSessionStage(getUpdate().getId(), stageCounter.incrementAndGet());
		stg.setName(action.name());
		stg.setStart(lastTimestamp);
		stg.setEnd(end);
		hub().emitTrace(stg);
		lastTimestamp = end;
	}
	
	public void handleError(Throwable thrw) {
		if(lastException != thrw) {
			var exp = exceptionTrace(thrw, -systemUTC().instant().toEpochMilli());
			hub().emitTrace(exp);
			lastException = thrw;
		}
	}

    static URI fromRequest(HttpServletRequest req) {
    	var url = req.getRequestURL().toString();
    	var qry = req.getQueryString();
        return create(isNull(qry) ? url : url + '?' + qry);
    }

	static String[] extractAllHeaderValues(HttpServletRequest request, String header) {
		var values = request.getHeaders(header);
		return isNull(values) || values.hasMoreElements()
				? null
				: list(values).stream()
				.flatMap(v-> stream(v.split(",")))
				.map(String::trim)
				.filter(not(String::isBlank))
				.toArray(String[]::new);
	}
	
	static UUID parseUUID(String id) {
		if(nonNull(id)) {
			try {
				return fromString(id);
			}
			catch (Exception e) {
				hub().reportMessage("HttpSessionTracer.parseUUID", "bad UUID");
			}
		}
		return null;
	}
}