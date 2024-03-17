package org.usf.traceapi.core;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * Simple trace MultiCaster impl. reduce bean dependencies
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraceMultiCaster {
	
    static final List<TraceHandler> handlers = new ArrayList<>();
    
	public static void register(TraceHandler sender) {
		handlers.add(sender);
	}
	
	static void emit(Session session) {
		handlers.forEach(h-> h.handle(session)); //non blocking..
	}
}
