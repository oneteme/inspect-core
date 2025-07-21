package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.usf.inspect.core.ConcurrentLinkedSetQueue.noQueue;
import static org.usf.inspect.core.DispatchState.DISPATCH;
import static org.usf.inspect.core.Helper.warnException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class EventTraceDispatcher implements Dispatcher {
	
	private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor(this::daemonThread);
    private final AtomicReference<DispatchState2> atomicState = new AtomicReference<>(DISPATCH);
    
	private final TracingProperties prop;
    private final DispatcherAgent agent;
    private final List<DispatchHook> hooks;
    private final ConcurrentLinkedSetQueue<EventTrace> queue;
    private int attempts;
    
	public EventTraceDispatcher(TracingProperties prop, SchedulingProperties schd, DispatcherAgent agent, List<DispatchHook> hooks) {
		this.prop = prop;
		this.agent = agent;
		this.hooks = unmodifiableList(hooks);
		this.queue = nonNull(agent) 
				? new ConcurrentLinkedSetQueueImpl<>()
				: noQueue(); //no agent => no queue
		this.executor.scheduleWithFixedDelay(()-> run(false), schd.getDelay(), schd.getDelay(), schd.getUnit());
		getRuntime().addShutdownHook(new Thread(this::complete, "shutdown-hook"));
	}
    
	void initialize(InstanceEnvironment env) {
		dispatch(h-> h.preRegister(this, env), 
    			()-> agent.register(env), 
    			h-> h.postRegister(this, env));
	}
	
    @Override
    public void emit(EventTrace trace) {
    	if(atomicState.get().canEmit()) {
    		dispatchIfCapacityExceeded(queue.add(trace));
    		hooks.forEach(h->{
    			try {
    				h.onTraceEmit(trace);
    			}
    			catch (Exception e) {
    				warnException(log, e, "error during emit hook {}" + h.getClass().getSimpleName());
    			}
    		});
    	}
    }

    @Override
    public void emitAll(EventTrace[] traces) { //server usage
    	if(atomicState.get().canEmit()) {
    		dispatchIfCapacityExceeded(queue.addAll(traces));
    		hooks.forEach(h->{
    			try {
    				h.onTracesEmit(traces);
    			}
    			catch (Exception e) {
    				warnException(log, e, "error during emit hook {}" + h.getClass().getSimpleName());
    			}
    		});
    	}
    }
    
    void dispatchIfCapacityExceeded(int size){
    	if(size >= prop.getQueueCapacity()) {
    		executor.submit(()-> this.run(false)); //deferred process
    	}
    }
    
	void run(boolean complete)  {
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
    }
    
    protected void dispatch(Consumer<DispatchHook> pre, Runnable process, Consumer<DispatchHook> post) {
    	var state = atomicState.get();
    	if(state.canEmit()) {
    		hooks.forEach(h->{
    			try {
    				pre.accept(h);
    			}
    			catch (Exception e) {
    				warnException(log, e, "error during pre-dispatch hook : {}", h.getClass().getSimpleName());
				}
    		});
    		if(state.canDispatch()) {
    			try {
    				process.run(); 
				}
    			catch (Exception e) {
    				warnException(log, e, "error during dispatch process");
    			}
    		}
        	else {
        		log.warn("cannot dispatch traces as the dispatcher state is {}", state);
        	}
			hooks.forEach(h->{
				try {
					post.accept(h);
				}
				catch (Exception e) {
					warnException(log, e, "error during post-dispatch hook : {}", h.getClass().getSimpleName());
				}
			});
		}
    }
    
    @Override
    public void tryDispatchQueue(int delay, BiFunction<List<EventTrace>, Integer, List<EventTrace>> cons) {
    	if(queue.size() > prop.getQueueCapacity()) {
    		dispatchQueue(delay, cons);
    	}
    }

    void dispatchQueue(int delay, BiFunction<List<EventTrace>, Integer, List<EventTrace>> cons) {
    	Collection<EventTrace> cs = queue.pop(); //set of traces
		var modifiable = new ArrayList<>(cs);
    	try {
    		var reQueue = extractPendingMetrics(delay, modifiable);
    		log.trace("dispatching {} traces, pending metrics : {}", modifiable.size(), reQueue.size());
    		reQueue.addAll(cons.apply(modifiable, reQueue.size()));
    		cs = reQueue; //back to queue
    	//catch DispatchException
    	} catch (Exception e) {
    		if(attempts % 5 == 0) {
    			warnException(log, e, "failed to dispatch {} items after {} attempts", cs.size(), attempts); //do not log exception stack trace
    		}
    	}
    	catch (OutOfMemoryError e) {
    		cs = emptyList(); //do not add items back to the queue, may release memory
    		log.error("out of memory error while dispatching {} items, those will be aborted", cs.size());
		}
    	finally {
    		if(!cs.isEmpty()) {
    			queue.requeueAll(cs); //go back to the queue (preserve order)
    		}
    	}
    }
    
    @Override
	public boolean dispatchNow(File file) {
    	try {
			agent.dispatch(file); //dispatch dump file
			log.debug("dump file {} dispatched", file.getName());
			return true;
		} catch (Exception e) {
			warnException(log, e, "cannot dispatch dump file {}", file.getName());
			return false;
		}
	}
    
    @Override
    public boolean dispatchNow(EventTrace[] traces) {
    	// TODO Auto-generated method stub
    	return false;
    }
   
    @Override
    public DispatchState2 getState() {
    	return atomicState.get();
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
	
	void complete() {
		atomicState.getAndUpdate(DispatchState2::complete);
    	log.info("shutting down the scheduler service...");
		executor.shutdown();
    	run(true);
	}
	
    public Stream<EventTrace> peek() {
    	return queue.peek();
    }
    
	static boolean isEmpty(List<?> arr) {
		return isNull(arr) || arr.isEmpty();
	}
	
	Thread daemonThread(Runnable r) {
		var thread = new Thread(r, "inspect-dispatcher");
 		thread.setDaemon(true);
 		thread.setUncaughtExceptionHandler((t,e)-> log.error("uncaught exception on thread {}", t.getName(), e));
		return thread;
	}
}
