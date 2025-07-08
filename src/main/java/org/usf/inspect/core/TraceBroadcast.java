package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.synchronizedArrayList;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
public final class TraceBroadcast {
	
    static final List<TraceHandler<Traceable>> handlers = synchronizedArrayList();
    
    private static final AtomicInteger counter = new AtomicInteger();
    
    static {
		getRuntime().addShutdownHook(new Thread(TraceBroadcast::complete, "shutdown-hook"));
    }
    
	public static void register(@NonNull TraceHandler<Traceable> handler) {
		handlers.add(handler);
	}
	
	public static void emit(Traceable trace) {
		System.err.println(counter.incrementAndGet());
		handlers.forEach(h->{
			try {
				h.handle(trace);
			} catch (Exception e) {
				log.warn("" + trace + " emit error {}", h, e);
			}
		});
	}
	
	public static void complete() {
		handlers.forEach(h->{
			try {
				h.complete();
			} catch (Exception e) {
				log.warn("complete error {}", h, e);
			}
		});
	}
}
