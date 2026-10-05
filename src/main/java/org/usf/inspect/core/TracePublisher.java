package org.usf.inspect.core;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.List;
/**
 * 
 * @author u$f
 *
 */
public interface TracePublisher {

    static final byte RETRY = 1;
    static final byte ABORT = 0;
    static final byte DEFER = -1;
	
	void register(InstanceEnvironment instance) throws DispatchException; //callback ?
    
	void flush(boolean complete, ProcessingQueue<EventTrace> queue) throws DispatchException;

	@Deprecated(forRemoval = true, since = "v1.2")
	default void dispatch(File dumpFile) {}

	default void mergeSessionMaskUpdates(List<EventTrace> traces){
		var call = traces.stream().mapMulti((t, c)-> {
			if(t instanceof AbstractSessionUpdate sc && sc.getRequestMask().get() > 0) {
				c.accept(sc.getId());
			}
		}).collect(toSet());
		var updt = traces.stream()
			.filter(SessionMaskUpdate.class::isInstance)
			.map(SessionMaskUpdate.class::cast)
			.filter(u-> !call.contains(u.getId()))
			.collect(toMap(SessionMaskUpdate::getId, identity(), (a,b)-> a.getMask() > b.getMask() ? a : b));
		traces.removeIf(t -> t instanceof SessionMaskUpdate u && !updt.containsKey(u.getId()));
	}
}