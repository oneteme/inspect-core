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
				log.warn("⨇ merging {} update traces for session {}, asyncDuration is {}", 
						e.getValue().size(), e.getKey(), prt.getDuration());
			}
			else {
				var finalDuration = upd.get().getAsyncDuration();
				if(finalDuration == -1) {
					long maxDuration = -1;
					if(e.getValue().size() == 1) {
						maxDuration = e.getValue().get(0).getDuration();
						if(maxDuration > 0) {
							log.debug("⨇ merging update trace for session {}, final asyncDuration is {}", 
									e.getKey(), finalDuration);
						}
						else {
							log.warn("⨇ merging update trace for session {}, final asyncDuration is {}, but update asyncDuration is {}", 
									e.getKey(), finalDuration, maxDuration);
						}
					}
					else {
						maxDuration = e.getValue().stream().max(comparing(SessionAsyncDurationUpdate::getDuration)).orElseThrow().getDuration();
						if(maxDuration > 0)  {
							log.warn("⨇ merging {} update traces for session {}, final asyncDuration is {}", 
									e.getValue().size(), e.getKey(), finalDuration);
						}
						else {
							log.warn("⨇ merging {} update traces for session {}, final asyncDuration is {}, but asyncDuration update is {}", 
									e.getValue().size(), e.getKey(), finalDuration, maxDuration);
						}
					}
					upd.get().setAsyncDuration(maxDuration);
				}
				else {
					log.warn("illegal state");
					continue;
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
				e.getValue().remove(prt); //keep only the last update
				log.debug("⨇ merging {} update traces for session {}, eventMask is {}", 
						e.getValue().size(), e.getKey(), prt.getMask());
			}
			else {
				var finalMask = upd.get().getEventMask();
				var prt = e.getValue().size() == 1 
						? e.getValue().get(0) 
						: e.getValue().stream().max(comparing(SessionMaskUpdate::getMask)).orElseThrow();
				var maxMask = prt.getMask();
				if(finalMask == maxMask) {
					log.debug("⨇ merging {} update traces for session {}, final eventMask is {}", 
							e.getValue().size(), e.getKey(), finalMask);
				}
				else {
					log.warn("⨇ merging {} update traces for session {}, final eventMask is {}, but eventMask update is {}", 
							e.getValue().size(), e.getKey(), finalMask, maxMask);
				}
			}
			traces.removeAll(e.getValue());
		}
	}
}