package org.usf.inspect.core;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
/**
 * 
 * @author u$f
 *
 */
public interface TracePublisher {
	
	static Logger log = getLogger(TracePublisher.class);

    static final byte RETRY = 1;
    static final byte ABORT = 0;
    static final byte DEFER = -1;
	
	void register(InstanceEnvironment instance) throws DispatchException; //callback ?
    
	void flush(boolean complete, ProcessingQueue<EventTrace> queue) throws DispatchException;

	@Deprecated(forRemoval = true, since = "v1.2")
	default void dispatch(File dumpFile) {}

	default void mergeTraces(List<EventTrace> traces){
		var map = traces.stream().<SessionMaskUpdate>mapMulti((t, c)-> {
			if(t instanceof SessionMaskUpdate sc) {
				c.accept(sc);
			}
		}).collect(groupingBy(SessionMaskUpdate::getId));
		for(var e : map.entrySet()) {
			var upd = traces.stream()
			.filter(t-> t instanceof AbstractSessionUpdate u && u.getId().equals(e.getKey()))
			.findAny();
			if(upd.isEmpty()) {
				var max = e.getValue().stream().max(comparing(SessionMaskUpdate::getMask));
				if(max.isPresent()) {
					e.getValue().remove(max.get()); 
					log.debug("merged {} traces for session {}, max mask is {}", e.getValue().size(), e.getKey(), max.get().getMask());
				}
				else {
					log.warn("unexpected empty traces for session {}, cannot merge", e.getKey());
				}
			}
			else {
				log.debug("merged {} traces for session {}, update mask is {}", e.getValue().size(), e.getKey(), ((AbstractSessionUpdate)upd.get()).getEventMask());
			}
			traces.removeAll(e.getValue());
		}
	}
}