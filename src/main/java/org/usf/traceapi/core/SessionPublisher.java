package org.usf.traceapi.core;

import static java.util.Collections.synchronizedList;
import static org.usf.traceapi.core.Helper.log;

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
public final class SessionPublisher {
	
    static final List<SessionHandler<Session>> handlers = synchronizedList(new ArrayList<>());
    
	public static void register(SessionHandler<Session> sender) {
		handlers.add(sender);
	}
	
	public static void emit(Session session) {
		handlers.forEach(h-> h.handle(session)); //non blocking..
	}
	
	public static void complete() {
		for(var h : handlers) {
			try {
				h.complete();
			} catch (Exception e) {
				log.warn("handler complete error {}", h, e);
			}
		}
	}
}
