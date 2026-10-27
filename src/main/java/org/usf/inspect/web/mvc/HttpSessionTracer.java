package org.usf.inspect.web.mvc;

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
import static org.usf.inspect.HttpAction.DELEGATION;
import static org.usf.inspect.HttpAction.EXECUTION;
import static org.usf.inspect.HttpAction.FINALIZATION;
import static org.usf.inspect.HttpAction.INITIALIZATION;
import static org.usf.inspect.HttpAction.TRANSMISSION;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.ExceptionTracer.emitExceptionTrace;
import static org.usf.inspect.core.SessionContextManager.createHttpSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.TraceHub.hub;
import static org.usf.inspect.web.WebUtils.TRACE_ID_HEADER;
import static org.usf.inspect.web.WebUtils.extractAuthScheme;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.usf.inspect.HttpAction;
import org.usf.inspect.HttpSessionSignal;
import org.usf.inspect.HttpSessionStage;
import org.usf.inspect.HttpSessionUpdate;
import org.usf.inspect.core.ExecutionTracer;
import org.usf.inspect.core.SafeCallable.SafeConsumer;
import org.usf.inspect.web.StreamCaptor.StreamExchangeListener;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;

/**
 * 
 * Filter → Interceptor.preHandle → Deferred(task-?) → Controller(dispatch) → Filter → Interceptor.preHandle → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
 * Filter → Interceptor.preHandle → Controller → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
 * 
 * @author u$f 
 *
 */
public final class HttpSessionTracer extends ExecutionTracer<Void> implements StreamExchangeListener {
	
	private final AtomicInteger stageCounter = new AtomicInteger();
	
	private Instant lastTimestamp;
	private Throwable lastException;
	private HttpSessionStage streamStage;
	
	@Setter
	private HttpServletResponse response;

	public HttpSessionTracer(HttpSessionSignal signal, HttpSessionUpdate update, SafeConsumer<HttpSessionSignal> cons) {
		super(signal, update, cons);
		this.lastTimestamp = signal.getStart();
	}
	
	public static HttpSessionTracer httpSessionTracer(HttpServletRequest request) {
		var sgn = createHttpSession(systemUTC().instant(), parseTraceUUID(request.getHeader(TRACE_ID_HEADER)));
		var upd = new HttpSessionUpdate(sgn.getId());
		return new HttpSessionTracer(sgn, upd, s->{
			s.setMethod(request.getMethod());
			s.setURI(fromRequest(request));
			s.setAuthScheme(extractAuthScheme(request.getHeader(AUTHORIZATION))); //extract user !?
			s.setDataSize(request.getContentLength());
			s.setContentEncoding(request.getHeader(CONTENT_ENCODING));
			s.setUserAgent(request.getHeader(USER_AGENT));
			s.setForwardedAddresses(extractAllHeaderValues(request, "X-Forwarded-For"));
		});
	}
	
	@Override
	public HttpSessionUpdate getUpdate() {
		return (HttpSessionUpdate) super.getUpdate();
	}
	
	@Override
	public void handle(Instant start, Instant end, Void obj, Throwable thrw) throws Exception {
		if(assertActiveTraceUpdate("HttpSessionTracer.handle")) {
			var upd = getUpdate();
			if(nonNull(streamStage)) {
				if(isNull(streamStage.getEnd())) { 
					streamStage.setEnd(end); //stream was not called
				}
				hub().emitTrace(streamStage);
				streamStage = null;
			}
			if(nonNull(response)){
				upd.setStatus((short)response.getStatus()); //check last exception
				upd.setContentType(response.getContentType());
				upd.setContentEncoding(response.getHeader(CONTENT_ENCODING)); 
				upd.setCacheControl(response.getHeader(CACHE_CONTROL));
				if(response instanceof InspectHttpServletResponseWrapper wrp) {
					upd.setDataSize(wrp.getWrittenBytes()); //real content size transfered 
				}
				else {
					var len = response.getHeader("Content-Length");
				    upd.setDataSize(nonNull(len) && !len.isEmpty() ? Long.parseLong(len) : -1);
				}
			}
			else {
				REPORTER.warn("HttpSessionTracer.handle: response is null");
			}
			super.handle(start, end, null, thrw);
		}
	}
	
	public void propagateContext() {
		if(assertActiveTraceUpdate("HttpSessionTracer.propagateContext")) {
			setActiveContext(getUpdate());
		}
	}

	public void emitInitializationStage(String name, String user, String location){
		if(emitStage(INITIALIZATION)) {
			var upd = getUpdate();
			upd.setName(name);
			upd.setUser(user);
			upd.setLocation(location);
		}
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

	boolean emitStage(HttpAction action) {
		if(assertActiveTraceUpdate("HttpSessionTracer.emitStage")) {
			var end = systemUTC().instant();
			var stg = new HttpSessionStage(getUpdate().getId(), stageCounter.incrementAndGet());
			stg.setName(action.name());
			stg.setStart(lastTimestamp);
			stg.setEnd(end);
			hub().emitTrace(stg);
			lastTimestamp = end;
			return true;
		}
		return false;
	}
	
	public void emitError(Throwable thrw) {
		if(assertActiveTraceUpdate("HttpSessionTracer.emitError")) {
			if(lastException != thrw) {
				emitExceptionTrace(thrw, getUpdate(), systemUTC().instant().toEpochMilli());
				lastException = thrw;
			}
		}
	}
	
	@Override
	public void onTransmissionStart() {
		if(assertActiveTraceUpdate("HttpSessionTracer.onTransmissionStart")) {
			if(isNull(streamStage)) {
				var s = systemUTC().instant();
				streamStage = new HttpSessionStage(getUpdate().getId(), stageCounter.incrementAndGet());
				streamStage.setStart(s);
				streamStage.setName(TRANSMISSION.name());
			}
			else {
				REPORTER.warn("HttpSessionTracer.onTransmissionStart: streamStage already started");
			}
		}
	}
	
	@Override
	public void onTransmissionEnd() {
		if(assertActiveTraceUpdate("HttpSessionTracer.onTransmissionEnd")) {
			if(nonNull(streamStage) && isNull(streamStage.getEnd())) {
				streamStage.setEnd(systemUTC().instant());
			}
			else {
				REPORTER.warn("HttpSessionTracer.onTransmissionEnd: streamStage already ended");
			}
		}
	}

    static URI fromRequest(HttpServletRequest req) {
    	var url = req.getRequestURL().toString();
    	var qry = req.getQueryString();
        return create(isNull(qry) ? url : url + '?' + qry); //change it
    }

	static String[] extractAllHeaderValues(HttpServletRequest request, String header) {
		var values = request.getHeaders(header);
		return isNull(values) || !values.hasMoreElements()
				? null
				: list(values).stream()
				.flatMap(v-> stream(v.split(",")))
				.map(String::trim)
				.filter(not(String::isBlank))
				.toArray(String[]::new);
	}
	
	static UUID parseTraceUUID(String id) {
		if(nonNull(id)) {
			try {
				return fromString(id);
			}
			catch (Exception e) {
				REPORTER.warn("Invalid trace id: {}", id, e);
			}
		}
		return null;
	}
}