package org.usf.inspect.util;

import static java.time.Clock.systemUTC;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.Helper.formatLocation;
import static org.usf.inspect.Helper.upperStackFrame;
import static org.usf.inspect.MainSessionType.SCHEDULE;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.Constants.SCHEDULING_ENABLED;
import static org.usf.inspect.core.ContextPropagators.wrapCallable;
import static org.usf.inspect.core.ContextPropagators.wrapRunnable;
import static org.usf.inspect.core.ExecutionTracer.mainSessionTracer;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.SessionContextManager.activeContext;

import java.lang.StackWalker.StackFrame;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.usf.inspect.core.ExecutionTracer;
import org.usf.inspect.core.SessionContext;

/**
 * 
 * @author u$f
 *
 */
public class ScheduledExecutorServiceWrapper extends ExecutorServiceWrapper implements ScheduledExecutorService {

	private final boolean traceSession; //trace schedule as MainSession
	
	ScheduledExecutorServiceWrapper(ScheduledExecutorService es, boolean traceSession) {
		super(es);
		this.traceSession = traceSession;
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
		return se().schedule(traceSession 
				? wrapAndTraceCallable(task) 
				: wrapCallable(task), delay, unit);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
		return se().schedule(traceSession 
				? wrapAndTraceRunnable(task) 
				: wrapRunnable(task), delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
		return se().scheduleAtFixedRate(traceSession 
				? wrapAndTraceRunnable(task) 
				: wrapRunnable(task), initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
		return se().scheduleWithFixedDelay(traceSession 
				? wrapAndTraceRunnable(task) 
				: wrapRunnable(task), initialDelay, delay, unit);
	}
	
	ScheduledExecutorService se() {
		return (ScheduledExecutorService) es;
	}
	
	static Runnable wrapAndTraceRunnable(Runnable task) {
		var cnt = new AtomicLong();
		var ctx = activeContext();
		var frm = upperStackFrame(ScheduledExecutorServiceWrapper.class);
		return ()-> exec(task::run, scheduleSessionTracer(cnt, ctx, frm));
	}
	
	static <T> Callable<T> wrapAndTraceCallable(Callable<T> task){
		var cnt = new AtomicLong();
		var ctx = activeContext();
		var frm = upperStackFrame(ScheduledExecutorServiceWrapper.class);
		return ()-> call(task::call, scheduleSessionTracer(cnt, ctx, frm));
	}
	
	static <T> ExecutionTracer<T> scheduleSessionTracer(AtomicLong counter, SessionContext ctx, StackFrame frame){
		return mainSessionTracer(systemUTC().instant(), SCHEDULE, sgn-> {
			if(nonNull(frame)) {
				sgn.setName(frame.getMethodName() + "#schedule_" + counter.incrementAndGet());
				sgn.setLocation(formatLocation(frame.getClassName(), frame.getMethodName()));
			}
			if(nonNull(ctx)) {
				sgn.setParentId(ctx.getSession().getId());
			}
		});
	}
	
	public static ScheduledExecutorService wrap(ScheduledExecutorService es) {
		return wrap(es, getConfiguration().isEnabled(SCHEDULING_ENABLED, false), null);
	}

	public static ScheduledExecutorService wrap(ScheduledExecutorService es, boolean traceSession) {
		return wrap(es, traceSession, null);
	}
	
	public static ScheduledExecutorService wrap(ScheduledExecutorService es, boolean traceSession, String beanName) {
		if(nonNull(es) && getConfiguration().isEnabled()){
			if(es instanceof ScheduledExecutorServiceWrapper) {
				REPORTER.warn("ScheduledExecutorServiceWrapper.wrap: {} is already wrapped", es);
			}
			else {
				logWrappingBean(requireNonNullElse(beanName, "scheduledExecutorService"), es.getClass());
				return new ScheduledExecutorServiceWrapper(es, traceSession);
			}
		}
		return es;
	}
}
