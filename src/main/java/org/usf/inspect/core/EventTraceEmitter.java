package org.usf.inspect.core;

import static java.util.Collections.synchronizedList;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.DispatchState.DISPATCH;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class EventTraceEmitter implements Runnable {
	
	private final List<EventHandler<EventTrace>> handlers = synchronizedArrayList();
	private final List<EventListener<DispatchState>> listeners = synchronizedArrayList();
    private final AtomicReference<DispatchState> stateRef = new AtomicReference<>(DISPATCH);

	public void addHandler(@NonNull EventHandler<EventTrace> handler) {
		handlers.add(handler);
	}
	
	public void addListener(@NonNull EventListener<DispatchState> listener) {
		listeners.add(listener);
	}
	
	public void emitTrace(EventTrace trace) {
		if(stateRef.get() != DISABLE) {
			handlers.forEach(h->{//!Async
				try {
					h.handle(trace); //trace == QUEUE | DISPATCH
				} catch (Exception e) {
					log.warn("{} handle trace={} error, [{}]:{}", 
							h.getClass(), trace, e.getClass(), e.getMessage());
				}
			});
		}
		else {
			log.warn("EventTraceEmitter is DISABLE, cannot emit trace={}", trace);
		}
	}
    
    @Override
    public void run() {
		emitEvent(stateRef.get(), false);
    }

    public void complete() {
		emitEvent(stateRef.getAndSet(DISABLE), true); //set to DISABLE, prevent further processing
    }
    
	void emitEvent(DispatchState state, boolean complete) {
		listeners.forEach(h->{ //!Async
			try {
				h.onEvent(state, complete);
			} catch (Exception e) {
				log.warn("{} emiter state={} error, [{}]:{}", 
						h.getClass(), state, e.getClass(), e.getMessage());
			}
		});
	}

	public void updateState(DispatchState state) {
		stateRef.set(state);
		log.info("dispatch state changed to {}", state);
	}

	static <T> List<T> synchronizedArrayList() {
		return synchronizedList(new ArrayList<>());
	}
}