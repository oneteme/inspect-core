package org.usf.traceapi.core;

import java.util.LinkedList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * Simple MultiCaster impl. reduce bean dependencies
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraceMultiCaster {
	
    private static final List<TraceHandler> handlers = new LinkedList<>();
    
	public static void register(TraceHandler sender) {
		handlers.add(sender);
	}
	
	static void emit(Session session) {
		handlers.forEach(h-> h.handle(session)); //non blocking..
	}
}
