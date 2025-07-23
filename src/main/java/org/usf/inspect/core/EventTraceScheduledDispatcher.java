package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.usf.inspect.core.DispatchState.DISPATCH;
import static org.usf.inspect.core.Helper.warnException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class EventTraceScheduledDispatcher implements Dispatcher {
	
	private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor(EventTraceScheduledDispatcher::daemonThread);
    private final AtomicReference<DispatchState2> atomicState = new AtomicReference<>(DISPATCH);
    private final AtomicBoolean atomicRunning = new AtomicBoolean(false);
    
	private final TracingProperties prop;
    private final DispatcherAgent agent;
    private final List<DispatchHook> hooks;
    private final ConcurrentLinkedSetQueue<EventTrace> queue;
    private int attempts;
    
	public EventTraceScheduledDispatcher(TracingProperties prop, SchedulingProperties schd, DispatcherAgent agent) {
		this(prop, schd, agent, emptyList());
	}
    
	public EventTraceScheduledDispatcher(TracingProperties prop, SchedulingProperties schd, DispatcherAgent agent, List<DispatchHook> hooks) {
		this.prop = prop;
		this.agent = agent;
		this.hooks = unmodifiableList(hooks);
		this.queue = new ConcurrentLinkedSetQueue<>();
		this.executor.scheduleWithFixedDelay(()-> synchronizedDispatch(false, false), schd.getDelay(), schd.getDelay(), schd.getUnit());
		getRuntime().addShutdownHook(new Thread(this::complete, "shutdown-hook"));
	}
    
	public boolean dispatch(InstanceEnvironment instance) { //synch dispatch
    	if(atomicState.get().canDispatch()) {
			triggerHooks(h-> h.onInstanceEmit(this, instance));
			try {
				agent.dispatch(instance);
				return true;
			} catch (Exception e) {
				warnException(log, e, "failed to dispatch instance {}", instance.getId());
			}
    	}
    	return false;
	}
	
    @Override
    public boolean emit(EventTrace trace) {
    	if(atomicState.get().canEmit()) {
    		triggerHooks(h-> h.onTracesEmit(trace));
    		dispatchIfCapacityExceeded(queue.add(trace));
    		return true;
    	}
    	return false;
    }

    @Override
    public boolean emitAll(EventTrace[] traces) { //server usage
    	if(atomicState.get().canEmit()) {
    		triggerHooks(h-> h.onTracesEmit(traces));
    		dispatchIfCapacityExceeded(queue.addAll(traces));
    		return true;
    	}
    	return false;
    }
    
    private void dispatchIfCapacityExceeded(int size){
    	if(size > prop.getQueueCapacity()) {
    		synchronizedDispatch(true, false);//deferred process
    	}
    }
    
    void synchronizedDispatch(boolean deferred, boolean complete) {
    	if(!atomicRunning.compareAndExchange(false, true) || complete) { //complete => force dispatch, deferred => after trace emit
    		if(deferred) {
        		log.trace("deferred dispatching traces ..");
    			executor.submit(()-> run(complete, ()-> {
    				log.trace("deferred dispatch end");
    				atomicRunning.set(false);
    			}));
    		}
    		else {
    			log.trace("scheduled dispatching traces ...");
    			run(complete, ()-> {
    				log.trace("scheduled dispatch end");
    				atomicRunning.set(false);
    			});
    		}
    	}
	}
    
	void run(boolean complete, Runnable callback) {
    	dispatch(h-> h.preDispatch(this), 
		()-> dispatchQueue(complete ? 0 : prop.getDelayIfPending(), (arr, pnd)->{ //send all if complete
    		agent.dispatch(complete, ++attempts, pnd, arr);
    		if(attempts > 1) { //more than one attempt
    			log.info("successfully dispatched {} items after {} attempts", arr.size(), attempts);
    		}
    		attempts=0;
    		return emptyList();
    	}), 
    	h-> h.postDispatch(this));
    	var n = queue.size() - prop.getQueueCapacity(); 
    	if(n > 0) { 
    		queue.removeNLast(n);
    	}
    	callback.run();
    }
    
    protected void dispatch(Consumer<DispatchHook> pre, Runnable process, Consumer<DispatchHook> post) {
    	var state = atomicState.get();
    	if(state.canPropagate()) {
    		if(nonNull(pre)) {
    			triggerHooks(pre);
    		}
    		if(state.canDispatch()) {
    			try {
    				process.run(); 
				}
    			catch (Exception e) {
    				warnException(log, e, "failed to dispatch traces");
    			}
    		}
        	else {
        		log.warn("cannot dispatch traces as the dispatcher state is {}", state);
        	}
    		if(nonNull(post)) {//reduce queue even when dispatcher.state = QUEUE
    			triggerHooks(post);
    		}
		}
    }
    
    void triggerHooks(Consumer<DispatchHook> post){
    	hooks.forEach(h->{
			try {
				post.accept(h);
			}
			catch (Exception e) {
				warnException(log, e, "failed to execute hook '{}'", h.getClass().getSimpleName());
			}
		});
    }
    
    @Override
    public void tryDispatchQueue(int delay, BiFunction<List<EventTrace>, Integer, List<EventTrace>> cons) {
    	var size = queue.size();
    	if(size > prop.getQueueCapacity() || (size > 0 && atomicState.get().wasCompleted())) {
    		dispatchQueue(delay, cons);
    	}
    }

    void dispatchQueue(int delay, BiFunction<List<EventTrace>, Integer, List<EventTrace>> cons) {
    	Collection<EventTrace> cs = queue.pop(); //return LinkedHashSet
		var modifiable = new ArrayList<>(cs);
    	try {
    		var reQueue = extractPendingMetrics(delay, modifiable);
    		log.debug("dispatching {}/{} traces ..", modifiable.size(), cs.size());
    		reQueue.addAll(cons.apply(modifiable, reQueue.size()));
    		cs = reQueue; //back to queue
    	//catch DispatchException
    	} catch (Exception e) {
			warnException(log, e, "failed to dispatch {}/{} traces", modifiable.size(), cs.size()); //do not log exception stack trace
    	}
    	catch (OutOfMemoryError e) {
    		cs = emptyList(); //do not add items back to the queue, may release memory
    		log.error("out of memory error while dispatching {} traces, those will be aborted", cs.size());
		}
    	finally {
    		if(!cs.isEmpty()) {
    			queue.requeueAll(cs); //go back to the queue (preserve order)
    		}
    	}
    }
    
    @Override
	public void dispatchNow(File file, Callback<Void> cons) { //deferred !? + callback
    	if(atomicState.get().canDispatch()) {
	    	executor.submit(()->{
	        	Throwable thrw = null;
	        	try {
	    			agent.dispatch(file); //dispatch dump file
	    			log.debug("dump file {} dispatched", file.getName());
	    		} catch (Throwable e) {
	    			thrw = e;
	    			warnException(log, e, "cannot dispatch dump file {}", file.getName());
	    		}
	        	finally {
	    			cons.accept(null, thrw);
	    		}
	    	});
    	}
	}
    
    @Override
    public void dispatchNow(EventTrace[] traces, Callback<Void> cons) { //deferred !? + callback
    	if(atomicState.get().canDispatch()) {
	    	executor.submit(()->{
	        	Throwable thrw = null;
	        	try {
	    			agent.dispatch(false, 0, 0, asList(traces)); //dispatch dump file
	    		} catch (Throwable e) {
	    			thrw = e;
	    			warnException(log, e, "cannot dispatch {} traces", traces.length);
	    		}
	        	finally {
	    			cons.accept(null, thrw);
	    		}
	    	});
    	}
    }
   
    @Override
    public DispatchState2 getState() {
    	return atomicState.get();
    }
    
    public void setState(DispatchState state) {
    	atomicState.set(state);
	}
	
	void complete() {
		atomicState.getAndUpdate(DispatchState2::complete);
    	log.info("shutting down the scheduler service...");
		executor.shutdown();
    	synchronizedDispatch(false, true);
	}
	
    public Stream<EventTrace> peek() {
    	return queue.peek();
    }
    
	static List<EventTrace> extractPendingMetrics(int seconds, List<EventTrace> traces) {
		var pending = new ArrayList<EventTrace>();
		if(seconds != 0 && !isEmpty(traces)) { //seconds=0 takes all traces
			var now = now();
			for(var it=traces.listIterator(); it.hasNext();) {
				if(it.next() instanceof CompletableMetric o) {
					o.runSynchronizedIfNotComplete(()-> {
						if(seconds > -1 && o.getStart().until(now, SECONDS) > seconds) {
							it.set(o.copy()); //do not put it in pending, will be sent later
							log.trace("pending trace will be sent now : {}", o);
						}
						else { //-1 => do not trace pending
							pending.add(o);
							it.remove();
							log.trace("pending trace will be sent later : {} ", o);
						}
					});
				}
			}
		}
		return pending; //reusable list
	}
    
	static boolean isEmpty(List<?> arr) {
		return isNull(arr) || arr.isEmpty();
	}
	
	static Thread daemonThread(Runnable r) { //counter !?
		var thread = new Thread(r, "inspect-dispatcher");
 		thread.setDaemon(true);
 		thread.setUncaughtExceptionHandler((t,e)-> log.error("uncaught exception on thread {}", t.getName(), e));
		return thread;
	}
}
