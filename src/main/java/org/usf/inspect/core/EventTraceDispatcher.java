package org.usf.inspect.core;

import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.DispatchState.DISPATCH;
import static org.usf.inspect.core.Helper.synchronizedArrayList;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class EventTraceDispatcher implements Runnable {
	
	private final List<EventTraceHandler<EventTrace>> handlers = synchronizedArrayList();
	private final List<DispatchListener> listeners = synchronizedArrayList();
    @Getter
    private volatile DispatchState state = DISPATCH;
    
    public <T extends EventTraceHandler<EventTrace> & DispatchListener> void register(@NonNull T o) {
    	registerHandler(o);
    	registerListener(o);
	}

	public void registerHandler(@NonNull EventTraceHandler<EventTrace> handler) {
		handlers.add(handler);
	}
	
	public void registerListener(@NonNull DispatchListener listener) {
		listeners.add(listener);
	}
	
	public void emit(EventTrace trace) {
		if(state != DISABLE) {
			handlers.forEach(h->{//!Async
				try {
					h.handle(trace); //trace == QUEUE | DISPATCH
				} catch (Exception e) {
					log.warn("{} emit error {}, while handling", h.getClass(), trace, e);
				}
			});
		}
		else {
			log.warn("rejected 1 new items, current dispatcher state: {}", state);
		}
	}
    
    @Override
    public void run() {
		triggerDispatch(state, false);
    }

    public void complete() {
    	var stt = state; //hold status before change to &DISABLE
		updateState(DISABLE); //stop handle traces
		triggerDispatch(stt, true);
    }
    
	void triggerDispatch(DispatchState state, boolean complete) {
		listeners.forEach(h->{ //!Async
			try {
				h.onDispatchEvent(state, complete);
			} catch (Exception e) {
				log.warn("{} emit error {}, while signal(state={},complte={})", h.getClass(), e);
			}
		});
	}

	public void updateState(DispatchState state) {
		if(this.state != state) {
			this.state = state;
			log.info("dispatcher state was changed to {}", state);
		}
	}
}