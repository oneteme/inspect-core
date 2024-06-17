package org.usf.traceapi.core;

import static java.util.Collections.synchronizedList;

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
	
    static final List<SessionHandler> handlers = synchronizedList(new ArrayList<>());
    
	public static void register(SessionHandler sender) {
		handlers.add(sender);
	}
	
	public static void emit(Session session) {
		handlers.forEach(h-> h.handle(session)); //non blocking..
	}
}
