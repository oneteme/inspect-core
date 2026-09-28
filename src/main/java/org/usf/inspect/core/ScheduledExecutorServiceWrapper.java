package org.usf.inspect.core;

import static java.lang.StackWalker.getInstance;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.ExecutionTracer.forLocalRequest;
import static org.usf.inspect.core.ExecutionTracer.forMainSession;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.SessionContextManager.createLocalRequest;
import static org.usf.inspect.core.SessionContextManager.createScheduleSession;
import static org.usf.inspect.core.SessionContextManager.nextId;
import static org.usf.inspect.core.SessionPropagator.wrapCallable;
import static org.usf.inspect.core.SessionPropagator.wrapRunnable;
import static org.usf.inspect.core.TraceHub.hub;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public class ScheduledExecutorServiceWrapper extends ExecutorServiceWrapper implements ScheduledExecutorService {

	private static final String SCHEDULE = "schedule";
	private static final String SCHEDULE_AT_FIXED_RATE = "scheduleAtFixedRate";
	private static final String SCHEDULE_WITH_FIXED_DELAY = "scheduleWithFixedDelay";

	private final boolean contextPropagationOnly;
	
	ScheduledExecutorServiceWrapper(ScheduledExecutorService es, boolean contextPropagationOnly) {
		super(es);
		this.contextPropagationOnly = contextPropagationOnly;
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
		if(contextPropagationOnly) {
			return se().schedule(wrapCallable(task), delay, unit);
		}
		var id = nextId();
		var tsk = aroundScheduleTask(task, SCHEDULE, id);
		return call(()-> se().schedule(tsk, delay, unit), localRequestTracer(SCHEDULE, id));
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
		if(contextPropagationOnly) {
			return se().schedule(wrapRunnable(task), delay, unit);
		}
		var id = nextId();
		var tsk = aroundScheduleTask(task, SCHEDULE, id);
		return call(()-> se().schedule(tsk, delay, unit), localRequestTracer(SCHEDULE, id));
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
		if(contextPropagationOnly) {
			return se().scheduleAtFixedRate(wrapRunnable(task), initialDelay, period, unit);
		}
		var id = nextId();
		var tsk = aroundScheduleTask(task, SCHEDULE_AT_FIXED_RATE, id);
		return call(()-> se().scheduleAtFixedRate(tsk, initialDelay, period, unit), localRequestTracer(SCHEDULE_AT_FIXED_RATE, id));
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
		if(contextPropagationOnly) {
			return se().scheduleWithFixedDelay(wrapRunnable(task), initialDelay, delay, unit);
		}
		var id = nextId();
		var tsk = aroundScheduleTask(task, SCHEDULE_WITH_FIXED_DELAY, id);
		return call(()-> se().scheduleWithFixedDelay(tsk, initialDelay, delay, unit), localRequestTracer(SCHEDULE_WITH_FIXED_DELAY, id));
	}
	
	Runnable aroundScheduleTask(Runnable task, String methodName, UUID requestId) {
		var cnt = new AtomicLong();
		return ()-> exec(task::run, scheduleSessionTracer(methodName, cnt, requestId));
	}
	
	<T> Callable<T> aroundScheduleTask(Callable<T> task, String methodName, UUID requestId){
		var cnt = new AtomicLong();
		return ()-> call(task::call, scheduleSessionTracer(methodName, cnt, requestId));
	}
	
	<T> ExecutionTracer<T> scheduleSessionTracer(String methodName, AtomicLong cnt, UUID requestId){
		return forMainSession(()-> { 
			var sgn = createScheduleSession(systemUTC().instant());
			sgn.setName(methodName + '#' + cnt.incrementAndGet());
			sgn.setLocation(formatLocation(se().getClass().getName(), methodName));
			sgn.setParentId(requestId);
			return sgn;
		});
	}
	
	<T> ExecutionTracer<T> localRequestTracer(String name, UUID id){
		var frm = getInstance(RETAIN_CLASS_REFERENCE).walk(fr-> fr
				.dropWhile(f-> f.getDeclaringClass() == this.getClass()) //skip ScheduledExecutorServiceWrapper
				.findFirst().orElse(null));
		return forLocalRequest(()-> {
			var sgn = createLocalRequest(systemUTC().instant(), id);
			sgn.setType(EXEC.name());
			sgn.setName(name);
			if(nonNull(frm)) {
				if(isNull(name)) {
					sgn.setName(frm.getMethodName());
				}
				sgn.setLocation(formatLocation(frm.getClassName(), frm.getMethodName()));
			}
			return sgn;
		});
	}
	
	ScheduledExecutorService se() {
		return (ScheduledExecutorService) es;
	}

	public static ScheduledExecutorService wrap(ScheduledExecutorService es, boolean contextPropagationOnly) {
		return wrap(es, contextPropagationOnly, null);
	}
	
	public static ScheduledExecutorService wrap(@NonNull ScheduledExecutorService es, boolean contextPropagationOnly, String beanName) {
		if(hub().isEnabled()){
			if(es.getClass() != ScheduledExecutorServiceWrapper.class) {
				logWrappingBean(requireNonNullElse(beanName, "scheduledExecutorService"), es.getClass());
				return new ScheduledExecutorServiceWrapper(es, contextPropagationOnly);
			}
			else {
				log.warn("{}: {} is already wrapped", beanName, es);
			}
		}
		return es;
	}
}
