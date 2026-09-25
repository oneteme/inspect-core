package org.usf.inspect.core;

import static java.lang.StackWalker.getInstance;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.time.Clock.systemUTC;
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

	ScheduledExecutorServiceWrapper(ScheduledExecutorService es) {
		super(es);
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
		return schedule(task, delay, unit, null);
	}
	
	public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit, String scheduleName) {
		var id = nextId();
		var scn = nonNull(scheduleName) ? scheduleName : SCHEDULE;
		var tsk = aroundScheduleTask(task, scn, SCHEDULE, id);
		return call(()-> se().schedule(tsk, delay, unit), localRequestTracer(scn, id));
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
		return schedule(task, delay, unit, null);
	}
	
	public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit, String scheduleName) {
		var id = nextId();
		var scn = nonNull(scheduleName) ? scheduleName : SCHEDULE;
		var tsk = aroundScheduleTask(task, scn, SCHEDULE, id);
		return call(()-> se().schedule(tsk, delay, unit), localRequestTracer(scn, id));
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
		return scheduleAtFixedRate(task, initialDelay, period, unit, null);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit, String scheduleName) {
		var id = nextId();
		var scn = nonNull(scheduleName) ? scheduleName : SCHEDULE_AT_FIXED_RATE;
		var tsk = aroundScheduleTask(task, scheduleName, SCHEDULE_AT_FIXED_RATE, id);
		return call(()-> se().scheduleAtFixedRate(tsk, initialDelay, period, unit), localRequestTracer(scn, id));
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
		return scheduleWithFixedDelay(task, initialDelay, delay, unit, null);
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit, String scheduleName) {
		var id = nextId();
		var scn = nonNull(scheduleName) ? scheduleName : SCHEDULE_WITH_FIXED_DELAY;
		var tsk = aroundScheduleTask(task, scn, SCHEDULE_WITH_FIXED_DELAY, id);
		return call(()-> se().scheduleWithFixedDelay(tsk, initialDelay, delay, unit), localRequestTracer(scn, id));
	}
	
	Runnable aroundScheduleTask(Runnable task, String name, String methodName, UUID parentId) {
		var cnt = new AtomicLong();
		var lct = formatLocation(se().getClass().getName(), methodName);
		return ()-> exec(task::run, scheduleSessionTracer(name, lct, cnt, parentId));
	}
	
	<T> Callable<T> aroundScheduleTask(Callable<T> task, String name, String methodName, UUID parentId){
		var cnt = new AtomicLong();
		var lct = formatLocation(se().getClass().getName(), methodName);
		return ()-> call(task::call, scheduleSessionTracer(name, lct, cnt, parentId));
	}
	
	<T> ExecutionTracer<T> scheduleSessionTracer(String name, String location, AtomicLong cnt, UUID parentId){
		return forMainSession(()-> { 
			var sgn = createScheduleSession(systemUTC().instant());
			sgn.setName(name + '-' + cnt.incrementAndGet());
			sgn.setLocation(location);
			sgn.setParentId(parentId);
			return sgn;
		});
	}
	
	<T> ExecutionTracer<T> localRequestTracer(String name, UUID pID){
		var frm = getInstance(RETAIN_CLASS_REFERENCE).walk(fr-> fr
				.dropWhile(f-> f.getDeclaringClass() == this.getClass()) //skip ScheduledExecutorServiceWrapper
				.findFirst().orElse(null));
		return forLocalRequest(()-> {
			var req = createLocalRequest(systemUTC().instant(), pID);
			req.setType(EXEC.name());
			req.setName(name);
			if(nonNull(frm)) {
				req.setLocation(formatLocation(frm.getClassName(), frm.getMethodName()));
			}
			return req;
		});
	}
	
	ScheduledExecutorService se() {
		return (ScheduledExecutorService) es;
	}

	public static ScheduledExecutorService wrap(ScheduledExecutorService es) {
		return wrap(es, null);
	}
	
	public static ScheduledExecutorService wrap(@NonNull ScheduledExecutorService es, String beanName) {
		if(hub().isEnabled()){
			if(es.getClass() != ScheduledExecutorServiceWrapper.class) {
				logWrappingBean(requireNonNullElse(beanName, "executorService"), es.getClass());
				return new ScheduledExecutorServiceWrapper(es);
			}
			else {
				log.warn("{}: {} is already wrapped", beanName, es);
			}
		}
		return es;
	}
}
