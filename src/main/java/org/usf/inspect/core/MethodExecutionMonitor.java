package org.usf.inspect.core;

import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.Helper.outerStackTraceElement;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.LocalRequestType.CACHE;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.Monitor.traceAroundMethod;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.SessionContextManager.createBatchSession;
import static org.usf.inspect.core.SessionContextManager.createLocalRequest;
import static org.usf.inspect.core.SpelEvaluator.evalMethodExpression;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.Ordered;
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
public class MethodExecutionMonitor implements Ordered {

	private final AspectUserProvider userProvider;
	
	public static <E extends Throwable> void trackRunnable(LocalRequestType type, String name, SafeRunnable<E> fn) throws E {
		trackCallble(type, name, fn);
	}

	public static <T, E extends Throwable> T trackCallble(LocalRequestType type, String name, SafeCallable<T,E> fn) throws E {
		var ste = outerStackTraceElement(); //optimize by avoiding creating stacktrace in traceAroundMethod
		return call(fn, traceAroundMethod(createLocalRequest(systemUTC().instant()), req->{
			req.setType(type.name());
			req.setName(nonNull(name) ? name : ste.map(StackTraceElement::getMethodName).orElse(null));
			req.setLocation(ste.map(e-> formatLocation(e.getClassName(), e.getMethodName())).orElse(null));
			//set user !
		}));
	}

	@Around("@annotation(TraceableStage) || @annotation(org.springframework.scheduling.annotation.Scheduled)") //batch <> TraceableStage
	Object aroundMethod(ProceedingJoinPoint point) throws Throwable {
		var ses = activeContext();
		return isNull(ses) || ses.wasCompleted() ? aroundJob(point) : aroundMethod(point, EXEC.name());
	}

	Object aroundJob(ProceedingJoinPoint point) throws Throwable {
		return call(point::proceed, traceAroundMethod(createBatchSession(systemUTC().instant()), ses-> {
			ses.setName(resolveStageName(point));
			ses.setLocation(locationFrom(point));
			ses.setUser(userProvider.getUser(point, ses.getName()));
		}));
	}

	@Around("@annotation(org.springframework.cache.annotation.Cacheable)")
	Object aroundCacheable(ProceedingJoinPoint point) throws Throwable {
		return aroundMethod(point, CACHE.name());
	}
	
	Object aroundMethod(ProceedingJoinPoint point, String type) throws Throwable {
		return call(point::proceed, traceAroundMethod(createLocalRequest(systemUTC().instant()), req->{
			req.setType(type);
			req.setName(resolveStageName(point));
			req.setLocation(locationFrom(point));
			req.setUser(userProvider.getUser(point, req.getName()));
		}));
	}

	@Override
	public int getOrder() { //before @Transactional
		return HIGHEST_PRECEDENCE;
	}
	
	static String resolveStageName(ProceedingJoinPoint point) {
		var sgn = (MethodSignature)point.getSignature();
		var ant = sgn.getMethod().getAnnotation(TraceableStage.class);
		if(nonNull(ant)) {
			return ant.name().isEmpty()
					? sgn.getName()
					: evalMethodExpression(ant.name(), point.getThis(), sgn.getMethod(), point.getArgs());
		}
		var cch = sgn.getMethod().getAnnotation(Cacheable.class);
		if(nonNull(cch)) {
			return cch.key().isEmpty()  
					? sgn.getName()
					: evalMethodExpression(cch.key(), point.getThis(), sgn.getMethod(), point.getArgs());
		}
		return sgn.getName(); //Scheduled
	}

	static String locationFrom(ProceedingJoinPoint point) {
		var sgn = point.getSignature();
		return formatLocation(sgn.getDeclaringTypeName(), sgn.getName());
	}
}
