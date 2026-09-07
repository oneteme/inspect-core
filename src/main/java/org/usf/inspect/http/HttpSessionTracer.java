package org.usf.inspect.http;

import static java.net.URI.create;
import static java.time.Clock.systemUTC;
import static java.util.Arrays.stream;
import static java.util.Collections.list;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.function.Predicate.not;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.USER_AGENT;
import static org.usf.inspect.core.Helper.extractAuthScheme;
import static org.usf.inspect.core.HttpAction.DEFERRED;
import static org.usf.inspect.core.HttpAction.POST_PROCESS;
import static org.usf.inspect.core.HttpAction.PRE_PROCESS;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.createHttpSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.TraceDispatcherHub.hub;
import static org.usf.inspect.http.WebUtils.TRACE_HEADER;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.usf.inspect.core.ExecutionTracer;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.core.HttpSessionStage;
import org.usf.inspect.core.HttpSessionUpdate;
import org.usf.inspect.core.TraceUpdate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
	
	private final HttpServletResponse response;
	private final BooleanSupplier isAsync;
	
	private Instant lastTimestamp;

	public HttpSessionTracer(TraceUpdate update, Instant start, HttpServletResponse response, BooleanSupplier isAsync) {
		super(update);
		this.response = response;
		this.isAsync = isAsync;
		this.lastTimestamp = start;
	}
	
	public static HttpSessionTracer httpSessionListener(HttpServletRequest request, HttpServletResponse response, BooleanSupplier isAsync) {
		var sgn = createHttpSession(systemUTC().instant(), parseUUID(request.getHeader(TRACE_HEADER)));
		var signal = traceSignal(()->{
			sgn.setMethod(request.getMethod());
			sgn.setURI(fromRequest(request));
			sgn.setAuthScheme(extractAuthScheme(request.getHeader(AUTHORIZATION))); //extract user !?
			sgn.setDataSize(request.getContentLength());
			sgn.setContentEncoding(request.getHeader(CONTENT_ENCODING));
			sgn.setUserAgent(request.getHeader(USER_AGENT));
			sgn.setForwardedAddresses(extractAllHeaderValues(request, "X-Forwarded-For"));
			if(nonNull(response)) {
				response.addHeader(TRACE_HEADER, sgn.getId().toString()); //add headers before doFilter
				response.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, TRACE_HEADER);
			}
			return sgn;
		});
		var upd = new HttpSessionUpdate(signal.getId());
		return new HttpSessionTracer(upd, signal.getStart(), response, isAsync);
	}
	
	@Override
	public HttpSessionUpdate getUpdate() {
		return (HttpSessionUpdate) super.getUpdate();
	}
	
	@Override
	public void handle(Instant start, Instant end, Void obj, Throwable thrw) throws Exception {
		if(assertActiveTraceUpdate("HttpSessionTracer.handle")) {
			var upd = getUpdate();
			if(isAsync.getAsBoolean()) {
				emitStage(DEFERRED);
				clearContext(upd);
			}
			else {
				upd.setStatus((short)response.getStatus());
				upd.setDataSize(response.getBufferSize()); //!exact size
				upd.setContentType(response.getContentType());
				upd.setContentEncoding(response.getHeader(CONTENT_ENCODING)); 
				upd.setCacheControl(response.getHeader(CACHE_CONTROL));
				super.handle(start, end, obj, thrw);
			}
		}
	}
	
	public void async() {
		setActiveContext(getUpdate()); //new Thread
	}

	public void preProcess(){
		emitStage(PRE_PROCESS);
	}
	
	public void process(){ //see this.asyncPostFilterHander
		emitStage(PROCESS);
	}

	public void postProcess(String name, String user, Throwable thrw){
		emitStage(POST_PROCESS);
		try{
			var upd = getUpdate();
			upd.setName(name);
			upd.setUser(user);
			if(nonNull(thrw)) {// unhandeled exception in @ControllerAdvice
				handleError(thrw);
			}
		}
		catch (Exception e) {
			hub().reportError(true, "HttpSessionMonitor.postProcess", e);
		}
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
		var exp = exceptionTrace(thrw, systemUTC().instant().toEpochMilli());
		hub().emitTrace(exp);
	}

    static URI fromRequest(HttpServletRequest req) {
    	var url = req.getRequestURL().toString();
    	var qry = req.getQueryString();
        return create(isNull(qry) ? url : url + '?' + qry);
    }

	static String[] extractAllHeaderValues(HttpServletRequest request, String header) {
		var values = request.getHeaders(header);
		return isNull(values) 
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
				hub().reportMessage(false, "HttpSessionTracer.parseUUID", "bad UUID");
			}
		}
		return null;
	}
}