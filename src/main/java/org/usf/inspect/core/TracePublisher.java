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
	
	static final Logger log = getLogger(TracePublisher.class);

    static final String RETRY = "1";
    static final String ABORT = "0";
	
	void register(InstanceEnvironment instance) throws DispatchException;
    
	void flush(boolean complete, ProcessingQueue<EventTrace> queue) throws DispatchException;

	@Deprecated(forRemoval = true, since = "v1.2")
	default void dispatch(File dumpFile) {}

	default void mergeTraceParts(List<EventTrace> traces){
		mergeMaskTraces(traces);
		mergeAsyncDurationTraces(traces);
	}
	
	static void mergeAsyncDurationTraces(List<EventTrace> traces) {
		var map = traces.stream().<SessionAsyncDurationUpdate>mapMulti((t, c)-> {
			if(t instanceof SessionAsyncDurationUpdate prt) {
				c.accept(prt);
			}
		}).collect(groupingBy(TracePart::getId));
		for(var e : map.entrySet()) {
			var upd = traces.stream()
			.filter(t-> t instanceof AbstractSessionUpdate u && u.getId().equals(e.getKey()))
			.map(AbstractSessionUpdate.class::cast)
			.findAny();
			if(upd.isEmpty()) {
				if(e.getValue().size() == 1) {
					continue;
				}
				var prt = e.getValue().stream().max(comparing(SessionAsyncDurationUpdate::getDuration)).orElseThrow(); //cannot be empty since e.getValue().size() > 1
				e.getValue().remove(prt); //keep only the last update
				log.warn("⨇ merging {} update traces for session {}, asyncDuration is {}", e.getValue().size(), e.getKey(), prt.getDuration());
			}
			else {
				var async = upd.get().getAsyncDuration();
				if(e.getValue().size() == 1) {
					var max = e.getValue().get(0).getDuration();
					if(max == async)  {
						log.debug("⨇ merging update trace for session {}, final asyncDuration is {}", e.getKey(), async);
					}
					else {
						log.warn("⨇ merging update trace for session {}, final asyncDuration is {}, but update asyncDuration is {}", e.getKey(), async, max);
					}
				}
				else {
					var prt = e.getValue().stream().max(comparing(SessionAsyncDurationUpdate::getDuration)).orElseThrow();
					var max = prt.getDuration();
					if(max == async)  {
						log.warn("⨇ merging {} update traces for session {}, final asyncDuration is {}", e.getValue().size(), e.getKey(), async);
					}
					else {
						log.warn("⨇ merging {} update traces for session {}, final asyncDuration is {}, but update asyncDuration is {}", e.getValue().size(), e.getKey(), async, max);
					}
				}
			}
			traces.removeAll(e.getValue());
		}
	}
	
	static void mergeMaskTraces(List<EventTrace> traces) {
		
		var map = traces.stream().<SessionMaskUpdate>mapMulti((t, c)-> {
			if(t instanceof SessionMaskUpdate upd) {
				c.accept(upd);
			}
		}).collect(groupingBy(TracePart::getId));
		for(var e : map.entrySet()) {
			var upd = traces.stream()
			.filter(t-> t instanceof AbstractSessionUpdate u && u.getId().equals(e.getKey()))
			.map(AbstractSessionUpdate.class::cast)
			.findAny();
			if(upd.isEmpty()) {
				if(e.getValue().size() == 1) {
					continue;
				}
				var prt = e.getValue().stream().max(comparing(SessionMaskUpdate::getMask)).orElseThrow();
				e.getValue().remove(prt); //exclude max from merge, it will be used as final update
				log.debug("⨇ merging {} update traces for session {}, eventMask is {}", e.getValue().size(), e.getKey(), prt.getMask());
			}
			else {
				var prt = e.getValue().size() == 1 
						? e.getValue().get(0) 
						: e.getValue().stream().max(comparing(SessionMaskUpdate::getMask)).orElseThrow();
				var max = prt.getMask();
				var fnl = upd.get().getEventMask();
				if(max == fnl) {
					log.debug("⨇ merging {} update traces for session {}, final eventMask is {}", e.getValue().size(), e.getKey(), fnl);
				}
				else {
					log.warn("⨇ merging {} update traces for session {}, final eventMask is {}, but update eventMask is {}", e.getValue().size(), e.getKey(), fnl, max);
				}
			}
			traces.removeAll(e.getValue());
		}
	}
}