package org.usf.inspect.core;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;
import static org.usf.inspect.core.SessionContext.asyncDurationInitialValue;

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
			var res = traces.stream()
			.filter(t-> t instanceof AbstractSessionUpdate u && u.getId().equals(e.getKey()))
			.map(AbstractSessionUpdate.class::cast)
			.findAny();
			if(res.isEmpty()) {
				if(e.getValue().size() == 1) {
					continue;
				}
				var prt = e.getValue().stream().max(comparing(SessionAsyncDurationUpdate::getDuration)).orElseThrow(); //cannot be empty since e.getValue().size() > 1
				e.getValue().remove(prt); //keep only the last update
				log.warn("⨇ merging {} update traces for session {}, asyncDuration will be updated to {}", 
						e.getValue().size(), e.getKey(), prt.getDuration());
			}
			else {
				var prt = e.getValue().size() == 1 
						? e.getValue().get(0) 
						: e.getValue().stream().max(comparing(SessionAsyncDurationUpdate::getDuration)).orElseThrow();
				var maxDuration = prt.getDuration();
				var upd = res.get();
				if(nonNull(upd.getAsyncDuration())) {
					if(upd.getAsyncDuration() == asyncDurationInitialValue || upd.getAsyncDuration() == maxDuration) {
						if(e.getValue().size() == 1) {
							log.debug("⨇ merging update trace for session {}, final asyncDuration is {}", e.getKey(), maxDuration);
						}
						else {
							log.warn("⨇ merging {} update traces for session {}, final asyncDuration is {}", e.getValue().size(), e.getKey(), maxDuration);
						}
						res.get().setAsyncDuration(maxDuration);
					}
					else{
						log.warn(ABORT);
					}
				}
				else {
					log.warn("");
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
					continue; //only one update, no need to merge
				}
				var maskUpdate = e.getValue().stream().max(comparing(SessionMaskUpdate::getMask)).orElseThrow();
				e.getValue().remove(maskUpdate); //keep only the last update
				log.debug("⨇ merging {} update traces for session {}, eventMask will be updated to {}",
						e.getValue().size(), e.getKey(), maskUpdate.getMask());
			}
			else {
				var finalMask = upd.get().getEventMask();
				var maskUpdate = e.getValue().size() == 1 
						? e.getValue().get(0) 
						: e.getValue().stream().max(comparing(SessionMaskUpdate::getMask)).orElseThrow();
				if(maskUpdate.getMask() == finalMask) {
					log.debug("⨇ merging {} update traces for session {}, final eventMask is {}", 
							e.getValue().size(), e.getKey(), finalMask);
				}
				else { //asynchronous update, keep the last update mask
					upd.get().setEventMask(maskUpdate.getMask());
					log.debug("⨇ merging {} update traces for session {}, final eventMask is {} and will be updated to {}",
							e.getValue().size(), e.getKey(), finalMask, maskUpdate.getMask());
				}
			}
			traces.removeAll(e.getValue());
		}
	}
}