package org.usf.inspect.core;

import static java.lang.StackWalker.getInstance;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.aop.support.AopUtils.getMostSpecificMethod;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.usf.inspect.core.ExecutionTracer.forLocalRequest;
import static org.usf.inspect.core.ExecutionTracer.forMainSession;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.LocalRequestType.CACHE;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.LocalRequestType.TRANSACTION;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.SessionContextManager.createLocalRequest;
import static org.usf.inspect.core.SessionContextManager.createScheduleSession;
import static org.usf.inspect.core.SpelEvaluator.evalMethodExpression;
import static org.usf.inspect.core.SpelEvaluator.evalMethodTemplate;
import static org.usf.inspect.core.TraceHub.hub;

import java.lang.StackWalker.StackFrame;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.Transactional;
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
		trackCallable(type, name, fn);
	}

	public static <T, E extends Throwable> T trackCallable(LocalRequestType type, String name, SafeCallable<T,E> fn) throws E {
		return call(fn, forLocalRequest(()->{
			var sgn = createLocalRequest(systemUTC().instant());
			var frm = upperStackFrame(MethodExecutionMonitor.class); //skip MethodExecutionMonitor
			sgn.setName(name);
			if(nonNull(type)) {
				sgn.setType(type.name());
			}
			if(nonNull(frm)) {
				if(isNull(sgn.getName())) {
					sgn.setName(frm.getMethodName());
				}
				sgn.setLocation(formatLocation(frm.getClassName(), frm.getMethodName()));
			}
			return sgn;
		}));
	}

	@Around("@annotation(TraceableStage)"
			+ " && !@annotation(org.springframework.scheduling.annotation.Scheduled)"
			+ " && !@annotation(org.springframework.scheduling.annotation.Schedules)") //batch <> TraceableStage
	Object aroundMethod(ProceedingJoinPoint point) throws Throwable {
		var ctx = activeContext();
		return isNull(ctx) ? aroundSchedule(point) : aroundMethod(point, EXEC.name());
	}
	
	@Around("@annotation(org.springframework.scheduling.annotation.Scheduled)"
			+ " || @annotation(org.springframework.scheduling.annotation.Schedules)")
	Object aroundSchedule(ProceedingJoinPoint point) throws Throwable {
		var ctx = activeContext();
		if(nonNull(ctx) && !ctx.isStartup()) { //startup context may still be active on early scheduled jobs
			hub().emitReport("MethodExecutionMonitor.aroundSchedule", "active session context found, but not completed");
		} //TODO : localRequest !? 
		return call(point::proceed, forMainSession(()-> { 
			var sgn = createScheduleSession(systemUTC().instant());
			sgn.setName(resolveStageName(point));
			sgn.setLocation(locationFrom(point));
			sgn.setUser(userProvider.getUser(point, sgn.getName()));
			return sgn;
		}));
	}
	
	

	@Around("(@annotation(org.springframework.cache.annotation.Cacheable) "
	        + "|| @within(org.springframework.cache.annotation.Cacheable) "
	        + "|| @annotation(org.springframework.cache.annotation.CachePut) "
	        + "|| @within(org.springframework.cache.annotation.CachePut) "
	        + "|| @annotation(org.springframework.cache.annotation.CacheEvict) "
	        + "|| @within(org.springframework.cache.annotation.CacheEvict) "
	        + "|| @annotation(org.springframework.cache.annotation.Caching) "
	        + "|| @within(org.springframework.cache.annotation.Caching) "
	        + "|| @annotation(org.springframework.transaction.annotation.Transactional) "
	        + "|| @within(org.springframework.transaction.annotation.Transactional)) "
	        + "&& !@annotation(TraceableStage)") 
	Object aroundCacheable(ProceedingJoinPoint point) throws Throwable {
		var typ = nonNull(findMethodOrClassAnnotation(targetMethod(point), Transactional.class)) ? TRANSACTION : CACHE;
		return aroundMethod(point, typ.name());
	}

	Object aroundMethod(ProceedingJoinPoint point, String type) throws Throwable {
		return call(point::proceed, forLocalRequest(()->{
			var sgn = createLocalRequest(systemUTC().instant());
			sgn.setType(type);
			sgn.setName(resolveStageName(point));
			sgn.setLocation(locationFrom(point));
			return sgn;
		}));
	}

	@Override
	public int getOrder() { //before @Transactional
		return HIGHEST_PRECEDENCE;
	}
	
	static String resolveStageName(ProceedingJoinPoint point) {
		var mth = targetMethod(point);
		var ant = findAnnotation(mth, TraceableStage.class);
		if(nonNull(ant)) { //keep TraceableStage over Cacheable/Transactional to allow name override
			return ant.name().isEmpty()
					? mth.getName()
					: evalMethodTemplate(ant.name(), point.getTarget(), mth, point.getArgs()); //template: "literal" | "prefix-#{#arg}"
		}
		var trx = findMethodOrClassAnnotation(mth, Transactional.class);
		if(nonNull(trx)) {
			return trx.label().length == 0
					? mth.getName()
					: String.join("_", trx.label());
		}
		var key = tryExtractCacheKey(mth);
		if(nonNull(key)) {
			return key.isEmpty()  
					? mth.getName()
					: evalMethodExpression(key, point.getTarget(), mth, point.getArgs()); //pure SpEL: "#id"
		}
		return mth.getName(); //Scheduled, Caching
	}
	
	private static String tryExtractCacheKey(Method method) {
		var cch = findMethodOrClassAnnotation(method, Cacheable.class);
		if(nonNull(cch)) {
			return cch.key();
		}
		var ccp = findMethodOrClassAnnotation(method, CachePut.class);
		if(nonNull(ccp)) {
			return ccp.key();
		}
		var cce = findMethodOrClassAnnotation(method, CacheEvict.class);
		if(nonNull(cce)) {
			return cce.key();
		}
		return null;
	} 
	
	static <T extends Annotation> T findMethodOrClassAnnotation(Method method, Class<T> annotation) {
		var trx = findAnnotation(method, annotation);
		return nonNull(trx) ? trx : findAnnotation(method.getDeclaringClass(), annotation);
	}
	
	/**
	 * implementation method (JDK proxy => signature returns the interface method)
	 */
	static Method targetMethod(ProceedingJoinPoint point) {
		var mth = ((MethodSignature)point.getSignature()).getMethod();
		var trg = point.getTarget();
		return nonNull(trg) ? getMostSpecificMethod(mth, trg.getClass()) : mth;
	}
	
	static String locationFrom(ProceedingJoinPoint point) {
		var mth = targetMethod(point);
		return formatLocation(mth.getDeclaringClass().getName(), mth.getName());
	}
	
	static StackFrame upperStackFrame(Class<?> clazz) {
		return getInstance(RETAIN_CLASS_REFERENCE).walk(fr-> fr
				.dropWhile(f-> f.getDeclaringClass() == clazz) //skip MethodExecutionMonitor
				.findFirst().orElse(null));
	}
}