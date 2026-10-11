package org.usf.inspect.core;

import static java.lang.StackWalker.getInstance;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.Constants.SCHEDULING_ENABLED;
import static org.usf.inspect.core.ContextPropagators.wrapCallable;
import static org.usf.inspect.core.ContextPropagators.wrapRunnable;
import static org.usf.inspect.core.ExecutionTracer.forMainSession;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.MainSessionType.SCHEDULE;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.TraceHub.hub;

import java.lang.StackWalker.StackFrame;
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

	private final boolean propagateContext; //reactor context propagation
	
	ScheduledExecutorServiceWrapper(ScheduledExecutorService es, boolean propagateContext) { //as MainSession
		super(es);
		this.propagateContext = propagateContext;
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
		var wrapper = propagateContext ? wrapCallable(task) : wrapAndTraceCallable(task);
		return se().schedule(wrapper, delay, unit);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
		var wrapper = propagateContext ? wrapRunnable(task) : wrapAndTraceRunnable(task);
		return se().schedule(wrapper, delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
		var wrapper = propagateContext ? wrapRunnable(task) : wrapAndTraceRunnable(task);
		return se().scheduleAtFixedRate(wrapper, initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
		var wrapper = propagateContext ? wrapRunnable(task) : wrapAndTraceRunnable(task);
		return se().scheduleWithFixedDelay(wrapper, initialDelay, delay, unit);
	}
	
	ScheduledExecutorService se() {
		return (ScheduledExecutorService) es;
	}
	
	static Runnable wrapAndTraceRunnable(Runnable task) {
		var cnt = new AtomicLong();
		var ctx = activeContext();
		var frm = upperStackFrame();
		return ()-> exec(task::run, scheduleSessionTracer(cnt, ctx, frm));
	}
	
	static <T> Callable<T> wrapAndTraceCallable(Callable<T> task){
		var cnt = new AtomicLong();
		var ctx = activeContext();
		var frm = upperStackFrame();
		return ()-> call(task::call, scheduleSessionTracer(cnt, ctx, frm));
	}
	
	static <T> ExecutionTracer<T> scheduleSessionTracer(AtomicLong counter, SessionContext ctx, StackFrame frame){
		return forMainSession(systemUTC().instant(), SCHEDULE, sgn-> {
			if(nonNull(frame)) {
				sgn.setName(frame.getMethodName() + "#schedule(" + counter.incrementAndGet() + ')');
				sgn.setLocation(formatLocation(frame.getClassName(), frame.getMethodName()));
			}
			if(nonNull(ctx)) {
				sgn.setParentId(ctx.getSession().getId());
			}
		});
	}
	
	static StackFrame upperStackFrame(){
		var sf = getInstance(RETAIN_CLASS_REFERENCE).walk(fr-> fr
				.dropWhile(f-> f.getDeclaringClass() == ScheduledExecutorServiceWrapper.class)
				.findFirst().orElse(null));
		if(isNull(sf)) {
			hub().emitReport("ScheduledExecutorServiceWrapper.upperStackFrame", "stackFrame is null");
		}
		return sf;
	}
	
	public static ScheduledExecutorService wrap(ScheduledExecutorService es) {
		return wrap(es, !getConfiguration().isEnabled(SCHEDULING_ENABLED, true), null);
	}

	public static ScheduledExecutorService wrap(ScheduledExecutorService es, boolean propagateContext) {
		return wrap(es, propagateContext, null);
	}
	
	public static ScheduledExecutorService wrap(@NonNull ScheduledExecutorService es, boolean propagateContext, String beanName) {
		if(getConfiguration().isEnabled()){
			if(es.getClass() != ScheduledExecutorServiceWrapper.class) {
				logWrappingBean(requireNonNullElse(beanName, "scheduledExecutorService"), es.getClass());
				return new ScheduledExecutorServiceWrapper(es, propagateContext);
			}
			else {
				log.warn("{}: {} is already wrapped", beanName, es);
			}
		}
		return es;
	}
}
