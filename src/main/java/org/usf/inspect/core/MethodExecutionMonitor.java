package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.evalExpression;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.Helper.outerStackTraceElement;
import static org.usf.inspect.core.LocalRequestType.CACHE;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.Monitor.executionHandler;
import static org.usf.inspect.core.Monitor.mainExecutionHandler;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.SessionContextManager.createBatchSession;
import static org.usf.inspect.core.SessionContextManager.createLocalRequest;

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
		var ste = outerStackTraceElement();
		return call(fn, executionHandler(createLocalRequest(now()), req->{
			req.setType(type.name());
			req.setName(nonNull(name) ? name : ste.map(StackTraceElement::getMethodName).orElse(null));
			req.setLocation(ste.map(e-> formatLocation(e.getClassName(), e.getMethodName())).orElse(null));
			//set user !
		}));
	}

	@Around("@annotation(TraceableStage)") //batch <> TraceableStage
	Object aroundTraceable(ProceedingJoinPoint point) throws Throwable {
		var ses = activeContext();
		return isNull(ses) || ses.wasCompleted() 
				? aroundBatch(point) 
				: aroundStage(point);
	}

	Object aroundBatch(ProceedingJoinPoint point) throws Throwable {
		return call(point::proceed, mainExecutionHandler(createBatchSession(null), ses-> {
			ses.setName(resolveStageName(point));
			ses.setLocation(locationFrom(point));
			ses.setUser(userProvider.getUser(point, ses.getName()));
		}));
	}

	Object aroundStage(ProceedingJoinPoint point) throws Throwable {
		return call(point::proceed, executionHandler(createLocalRequest(now()), req->{
			req.setType(EXEC.name());
			req.setName(resolveStageName(point));
			req.setLocation(locationFrom(point));
			//set user !
		}));
	}

	@Around("@annotation(org.springframework.cache.annotation.Cacheable)")
	Object aroundCacheable(ProceedingJoinPoint point) throws Throwable {
		return call(point::proceed, executionHandler(createLocalRequest(now()), req->{
			req.setType(CACHE.name());
			req.setName(getCacheableName(point));
			req.setLocation(locationFrom(point));
			//set user !
		}));
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
