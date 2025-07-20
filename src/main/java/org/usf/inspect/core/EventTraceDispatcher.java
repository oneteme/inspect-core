package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.usf.inspect.core.DispatchState.DISPATCH;
import static org.usf.inspect.core.InspectContext.context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class EventTraceDispatcher implements Dispatcher, Runnable {
	
	private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor(this::daemonThread);
	private final ConcurrentLinkedSetQueue<EventTrace> queue = new ConcurrentLinkedSetQueue<>();
    private final AtomicReference<DispatchState> atomicState = new AtomicReference<>(DISPATCH);
    
	private final TracingProperties prop;
    private final ObjectDumper traceDumper; //optional, can be null
    private final DispatcherAgent agent;
    private int attempts;
    
	public EventTraceDispatcher(TracingProperties prop, SchedulingProperties schd, DispatcherAgent agent, ObjectMapper mapper) {
		this.traceDumper = new ObjectDumper(mapper, prop.getDumpDirectory());
		this.prop = prop;
		this.agent = agent;
		this.executor.scheduleWithFixedDelay(this, schd.getDelay(), schd.getDelay(), schd.getUnit());
		getRuntime().addShutdownHook(new Thread(this::complete, "shutdown-hook"));
	}
    
    @Override
    public void emit(EventTrace obj) {
    	dispatchIfCapacityExceeded(queue.add(obj));
    }

    @Override
    public void emitAll(EventTrace[] arr) { //server usage
    	dispatchIfCapacityExceeded(queue.addAll(arr));
    }
    
    void dispatchIfCapacityExceeded(int size){
    	if(size >= prop.getQueueCapacity()) {
    		executor.submit(this); //deferred process
    	}
    }
    
    @Override
	public void run()  {
    	if(atomicState.get() == DISPATCH) {
			dispatch(false);
		}
    	else {
    		log.warn("cannot emit traces as the dispatcher state is {}, current queue size: {}", atomicState.get(), queue.size());
    	} //state != DISPATCH || attempts > 0 
    	if(queue.size() > prop.getQueueCapacity()) { 
    		dumpQueueTraces(); //dump traces if queue size exceeds capacity
    	} //dump fail
    	if(queue.size() > prop.getQueueCapacity()) { //will be sent later
    		queue.removeIf(t-> t instanceof CompletableMetric c && !c.wasCompleted());
    	}
    	if(queue.size() > prop.getQueueCapacity()) {
    		queue.removeIf(t-> t instanceof LogEntry);
    	}
    	if(queue.size() > prop.getQueueCapacity()) { //lost details but keep main info
    		queue.removeIf(t-> t instanceof AbstractStage);
    	}
    	var n = queue.size() - prop.getQueueCapacity(); 
    	if(n > 0) { 
    		queue.removeNLast(n);
    	}
    }
	
    void dispatch(boolean complete) {
    	dispatchDumpFiles(); //dispatch dump files
    	var cs = queue.pop();
    	log.trace("dispatching {} items, current queue size: {}", cs.size(), queue.size());
    	try {
    		var modifiable = new ArrayList<>(cs);
    		var pending = extractPendingMetrics(complete ? 0 : prop.getDelayIfPending(), modifiable); //send all if complete
    		agent.dispatch(complete, ++attempts, pending.size(), modifiable);
    		if(attempts > 1) { //more than one attempt
    			log.info("successfully dispatched {} items after {} attempts", cs.size(), attempts);
    		}
    		attempts=0;
    		cs = pending; //keep pending traces for next dispatch
    	}
    	catch (Exception e) {// do not throw exception : retry later
    		if(attempts % 5 == 0) {
    			log.warn("failed to dispatch {} items after {} attempts, cause: [{}] {}", 
    					cs.size(), attempts, e.getClass().getSimpleName(), e.getMessage()); //do not log exception stack trace
    		}
    	}
    	catch (OutOfMemoryError e) {
    		cs = emptyList(); //do not add items back to the queue, may release memory
    		attempts = 0;
    		log.error("out of memory error while dispatching {} items, those will be aborted", cs.size());
    	}
    	finally { //TODO dump file after 100 attempts or 90% max buffer size
    		if(!cs.isEmpty()) {
    			queue.requeueAll(cs); //go back to the queue (preserve order)
    		}
    	}
    }

    @Override
    public void tryReduceQueue(int delay, BiFunction<List<EventTrace>, Integer, List<EventTrace>> cons) {
    	if(queue.size() > prop.getQueueCapacity()) {
        	List<EventTrace> trcs = new ArrayList<>(queue.pop());
        	try {
        		var arr = extractPendingMetrics(delay, trcs);
        		arr.addAll(cons.apply(trcs, prop.getQueueCapacity()));
        		trcs = arr; //back to queue
        	} catch (Exception e) {
        		context().reportError("cannot dump " + trcs.size() + " traces", e);
        	}
        	finally {
        		if(!trcs.isEmpty()) {
        			queue.requeueAll(trcs);
        		}
        	}
    	}
    }
    
    @Override
	public boolean dispatch(File file) {
    	try {
			agent.dispatch(file); //dispatch dump file
			log.debug("dump file {} dispatched", file.getName());
			return true;
		} catch (Exception e) {
			log.warn("cannot dispatch dump file {}, cause: [{}] {}", file.getName(), e.getClass().getSimpleName(), e.getMessage());
			return false;
		}
	}
   
    @Override
    public DispatchState getState() {
    	return atomicState.get();
    }
    
    @Override
    public boolean isQueueCapacityExceeded() {
    	return queue.size() >= prop.getQueueCapacity();
    }
    
    
    
	static List<EventTrace> extractPendingMetrics(int seconds, List<EventTrace> traces) {
		if(seconds != 0 && !isEmpty(traces)) { //seconds=0 takes all traces
			var pending = new ArrayList<EventTrace>();
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
			return pending;
		}
		return emptyList();
	}
	
	void complete() {
    	log.info("shutting down the scheduler service...");
		this.executor.shutdown();
		this.dispatch(true); //dump if fail !?
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
