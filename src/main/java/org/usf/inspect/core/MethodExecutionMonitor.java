package org.usf.inspect.core;

import static java.lang.StackWalker.getInstance;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.aop.support.AopUtils.getMostSpecificMethod;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.usf.inspect.core.AnnotationMonitoringProperties.INSPECT_TRACEABLE;
import static org.usf.inspect.core.AnnotationMonitoringProperties.SPRING_CAHCE;
import static org.usf.inspect.core.AnnotationMonitoringProperties.SPRING_SCHEDULE;
import static org.usf.inspect.core.AnnotationMonitoringProperties.SPRING_TRANSACTION;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.ExecutionTracer.forLocalRequest;
import static org.usf.inspect.core.ExecutionTracer.forMainSession;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.LocalRequestType.CACHE;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.LocalRequestType.TRANSACTION;
import static org.usf.inspect.core.MainSessionType.SCHEDULE;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.SpelEvaluator.evalMethodExpression;
import static org.usf.inspect.core.SpelEvaluator.evalMethodTemplate;

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

/**
 * 
 * @author u$f
 *
 */
@Aspect
public class MethodExecutionMonitor implements Ordered {

	private final AspectUserProvider userProvider;
	private final boolean ignoreCache;
	private final boolean ignoreSchedule;
	private final boolean ignoreTransaction;
	private final boolean ignoreTraceable;
	
	public MethodExecutionMonitor(AspectUserProvider userProvider, AnnotationMonitoringProperties properties) {
		this.userProvider = userProvider;
		this.ignoreCache = properties.getExcludes().contains(SPRING_CAHCE);
		this.ignoreSchedule = properties.getExcludes().contains(SPRING_SCHEDULE);
		this.ignoreTransaction = properties.getExcludes().contains(SPRING_TRANSACTION);
		this.ignoreTraceable = properties.getExcludes().contains(INSPECT_TRACEABLE);
	}

	public static <E extends Throwable> void trackRunnable(LocalRequestType type, String name, SafeRunnable<E> fn) throws E {
		trackCallable(type, name, fn);
	}

	public static <T, E extends Throwable> T trackCallable(LocalRequestType type, String name, SafeCallable<T,E> fn) throws E {
		if(getConfiguration().isEnabled()) {
			return fn.call();
		}
		return call(fn, forLocalRequest(systemUTC().instant(), sgn->{
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
		}));
	}

	@Around("@annotation(TraceableStage) "
			+ "&& !@annotation(org.springframework.scheduling.annotation.Scheduled) "
			+ "&& !@annotation(org.springframework.scheduling.annotation.Schedules)") //batch <> TraceableStage
	Object aroundMethod(ProceedingJoinPoint point) throws Throwable {
		if(ignoreTraceable) {
			return point.proceed();
		}
		var ctx = activeContext();
		return isNull(ctx) ? aroundSchedule(point) : aroundMethod(point, EXEC);
	}
	
	@Around("@annotation(org.springframework.scheduling.annotation.Scheduled) "
			+ "|| @annotation(org.springframework.scheduling.annotation.Schedules)")
	Object aroundSchedule(ProceedingJoinPoint point) throws Throwable {
		if(ignoreSchedule) {
			return point.proceed();
		}
		var ctx = activeContext();
		if(nonNull(ctx) && !ctx.isStartup()) { //startup context may still be active on early scheduled jobs
			REPORTER.warn("MethodExecutionMonitor.aroundSchedule: active session context found, but not completed");
		}
		return call(point::proceed, forMainSession(systemUTC().instant(), SCHEDULE, sgn-> { 
			sgn.setName(resolveStageName(point));
			sgn.setLocation(locationFrom(point));
			sgn.setUser(userProvider.getUser(point, sgn.getName()));
		}));
	}

	@Around("@annotation(org.springframework.cache.annotation.Cacheable) "
	        + "|| @within(org.springframework.cache.annotation.Cacheable) "
	        + "|| @annotation(org.springframework.cache.annotation.CachePut) "
	        + "|| @within(org.springframework.cache.annotation.CachePut) "
	        + "|| @annotation(org.springframework.cache.annotation.CacheEvict) "
	        + "|| @within(org.springframework.cache.annotation.CacheEvict) "
	        + "|| @annotation(org.springframework.cache.annotation.Caching) "
	        + "|| @within(org.springframework.cache.annotation.Caching) "
	        + "&& !@annotation(TraceableStage)") 
	Object aroundCacheable(ProceedingJoinPoint point) throws Throwable {
		if(ignoreCache) {
			return point.proceed();
		}
		return aroundMethod(point, CACHE);
	}
	
	@Around("(@annotation(org.springframework.transaction.annotation.Transactional) "
	        + "|| @within(org.springframework.transaction.annotation.Transactional)) "
	        + "&& !@annotation(TraceableStage)") 
	Object aroundTransactionnal(ProceedingJoinPoint point) throws Throwable {
		if(ignoreTransaction) {
			return point.proceed();
		}
		return aroundMethod(point, TRANSACTION);
	}

	Object aroundMethod(ProceedingJoinPoint point, LocalRequestType type) throws Throwable {
		return call(point::proceed, forLocalRequest(systemUTC().instant(), sgn->{
			sgn.setType(type.name());
			sgn.setName(resolveStageName(point));
			sgn.setLocation(locationFrom(point));
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