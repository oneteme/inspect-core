package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.evalExpression;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.Helper.outerStackTraceElement;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.LocalRequestType.CACHE;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.createBatchSession;
import static org.usf.inspect.core.SessionContextManager.createLocalRequest;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;

import java.util.function.Supplier;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.Ordered;
import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.SafeCallable.SafeRunnable;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Aspect
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class MethodExecutionMonitor implements Monitor, Ordered {

	private final AspectUserProvider userProvider;
	
	public static <E extends Throwable> void trackRunnable(LocalRequestType type, String name, SafeRunnable<E> fn) throws E {
		trackCallble(type, name, fn);
	}

	public static <T, E extends Throwable> T trackCallble(LocalRequestType type, String name, SafeCallable<T,E> fn) throws E {
		var ste = outerStackTraceElement();
		return call(fn, localRequestHandler(type, 
				()-> nonNull(name) ? name : ste.map(StackTraceElement::getMethodName).orElse(null),
				()-> ste.map(e-> formatLocation(e.getClassName(), e.getMethodName())).orElse(null), 
				()-> null, context()));
	}

	@Around("@annotation(TraceableStage)") //batch <> TraceableStage
	Object aroundTraceable(ProceedingJoinPoint point) throws Throwable {
		var ses = activeContext();
		return isNull(ses) || ses.wasCompleted() 
				? aroundBatch(point, context()) 
				: aroundStage(point, context());
	}

	Object aroundBatch(ProceedingJoinPoint point, Context ctx) throws Throwable {
		var call = createBatchSession(now(), ses->{
			ses.setName(resolveStageName(point));
			ses.setLocation(locationFrom(point));
			ses.setUser(userProvider.getUser(point, ses.getName()));
		});
		setActiveContext(call);
		return call(point::proceed, (s,e,o,t)-> {
			if(assertStillOpened(call)) {
				call.setStart(s);
				if(nonNull(t)) {
					call.setException(fromException(t));
				}
				call.setEnd(e);
				emit(call);
				clearContext(call);
			}
		});
	}

	Object aroundStage(ProceedingJoinPoint point, Context ctx) throws Throwable {
		return call(point::proceed, localRequestHandler(EXEC, 
				()-> resolveStageName(point),
				()-> locationFrom(point),
				()-> null, ctx));
	}

	@Around("@annotation(org.springframework.cache.annotation.Cacheable)")
	Object aroundCacheable(ProceedingJoinPoint point) throws Throwable {
		return call(point::proceed, localRequestHandler(CACHE, 
				()-> getCacheableName(point),
				()-> locationFrom(point),
				()-> null, context()));
	}

	public static <T> ExecutionHandler<T> localRequestHandler(LocalRequestType type, Supplier<String> nameSupp, Supplier<String> locationSupp, Supplier<String> userSupp, Context ctx) {
		var call = createLocalRequest(now(), req->{
			req.setType(type.name());
			req.setName(nameSupp.get());
			req.setLocation(locationSupp.get());
			req.setUser(userSupp.get());
		});
		return (s,e,o,t)-> {
			call.setStart(s);
			if(nonNull(t)) {
				call.setException(fromException(t));
			}
			call.setEnd(e);
			ctx.emitTrace(call);
		};
	}

	@Override
	public int getOrder() { //before @Transactional
		return HIGHEST_PRECEDENCE;
	}

	static String getCacheableName(ProceedingJoinPoint point) {
		var sgn = (MethodSignature)point.getSignature();
		var ant = sgn.getMethod().getAnnotation(Cacheable.class);
		return ant.key().isEmpty() ? sgn.getName() : ant.key();
	}
	
	static String resolveStageName(ProceedingJoinPoint point) {
		var sgn = (MethodSignature)point.getSignature();
		var ant = sgn.getMethod().getAnnotation(TraceableStage.class);
		if(!ant.name().isEmpty()) {
			try {
				return evalExpression(ant.name(), point.getThis(), sgn.getDeclaringType(),  
		        		sgn.getParameterNames(), point.getArgs()).toString();
			}
			catch (Exception e) {
				log.warn("cannot eval expression ='%s' on %s.%s", 
						ant.name(), sgn.getDeclaringType().getSimpleName(), sgn.getName());
			}
		}
		return sgn.getName();
	}

	static String locationFrom(ProceedingJoinPoint point) {
		var sgn = point.getSignature();
		return formatLocation(sgn.getDeclaringTypeName(), sgn.getName());
	}
}
