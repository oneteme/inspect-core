package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.evalExpression;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.LocalRequest.formatLocation;
import static org.usf.inspect.core.LocalRequestType.CACHE;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.SessionManager.asynclocalRequestListener;
import static org.usf.inspect.core.SessionManager.createBatchSession;
import static org.usf.inspect.core.SessionManager.currentSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.Ordered;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class MethodExecutionMonitor implements Ordered {

	private final AspectUserProvider userProvider;

	@Around("@annotation(TraceableStage)")
	Object aroundTraceable(ProceedingJoinPoint point) throws Throwable {
		var ses = currentSession();
		return isNull(ses) || ses.wasCompleted() 
				? aroundBatch(point) 
				: aroundStage(point);
	}

	Object aroundBatch(ProceedingJoinPoint point) throws Throwable {
		var ses = createBatchSession();
		call(()->{
			ses.setStart(now());
			ses.setThreadName(threadName());        
			ses.setName(resolveStageName(point));
			ses.setLocation(locationFrom(point));
			ses.setUser(userProvider.getUser(point, ses.getName()));
			return ses.updateContext();
		});
		return call(point::proceed, (s,e,o,t)-> {
			ses.runSynchronized(()-> {
				if(nonNull(t)) {
					ses.setException(fromException(t));
				}
				ses.setEnd(e);
			});
			return ses.releaseContext();
		});
	}

	Object aroundStage(ProceedingJoinPoint point) throws Throwable {
		return call(point::proceed, asynclocalRequestListener(EXEC, 
				()-> locationFrom(point),
				()-> resolveStageName(point)));
	}

	@Around("@annotation(org.springframework.cache.annotation.Cacheable)")
	Object aroundCacheable(ProceedingJoinPoint point) throws Throwable {
		return call(point::proceed, asynclocalRequestListener(CACHE, 
				()-> locationFrom(point),
				()-> getCacheableName(point)));
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
