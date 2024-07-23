package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.synchronizedArrayList;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 
 * Simple session publisher, reduce bean dependencies
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SessionPublisher {
	
    static final List<SessionHandler<Session>> handlers = synchronizedArrayList();
    
	public static void register(@NonNull SessionHandler<Session> sender) {
		handlers.add(sender);
	}
	
	public static void emit(Session session) {
		for(var h : handlers) {
			try {
				h.handle(session);
			} catch (Exception e) {
				log.warn("handel error {}", h, e);
			}
		}
	}
	
	public static void complete() {
		for(var h : handlers) {
			try {
				h.complete();
			} catch (Exception e) {
				log.warn("complete error {}", h, e);
			}
		}
	}
}
