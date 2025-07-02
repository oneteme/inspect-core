package org.usf.inspect.core;

import static java.lang.Runtime.getRuntime;
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
public final class MetricsBroadcast {
	
    static final List<SessionHandler<Metric>> handlers = synchronizedArrayList();
    
    static {
		getRuntime().addShutdownHook(new Thread(MetricsBroadcast::complete, "shutdown-hook"));
    }
    
	public static void register(@NonNull SessionHandler<Metric> sender) {
		handlers.add(sender);
	}
	
	public static void emit(Metric metric) {
		handlers.forEach(h->{
			try {
				h.handle(metric);
			} catch (Exception e) {
				log.warn("" + metric + " emit error {}", h, e);
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
