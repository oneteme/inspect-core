package org.usf.inspect.core;

import static java.util.Collections.synchronizedMap;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class EventTraceDebugger implements DispatchHook { //inspect.client.log : SESSION | REQUEST | STAGE
	
	private static final Comparator<? super Metric> METRIC_COMPARATOR = comparing(Metric::getStart);
	
	private Map<String, AbstractSession> sessions = synchronizedMap(new HashMap<>());
	private Map<String, Set<AbstractRequest>> requests = synchronizedMap(new HashMap<>());
	private Map<String, Set<AbstractStage>> stages = synchronizedMap(new HashMap<>());
	private Map<String, Set<LogEntry>> logs = synchronizedMap(new HashMap<>());

	@Override
	public void onTraceEmit(EventTrace t) {
		switch (t) {
			case AbstractSession s-> {
				if(s.wasCompleted()) {
					printSession(s);
					return;
				}
				sessions.put(s.getId(), s);
			}
			case AbstractRequest r-> appendTrace(requests, r.getSessionId(), r);
			case AbstractStage s-> appendTrace(stages, s.getRequestId(), s);
			case LogEntry e when nonNull(e.getSessionId()) -> appendTrace(logs, e.getSessionId(), e);
			default-> log.debug(">{}", t);
		}
    }

	@Override
	public void onDispatch(boolean complete, EventTrace[] traces)  {
		if(complete) {
			log.warn("unfinished tasks {}", sessions.size());
			sessions.values().forEach(this::printSession);
		}
	}
	
	synchronized void printSession(AbstractSession ses) {
		sessions.remove(ses.getId());
		log.debug(">{}",ses);
		printMap(stages, ses.getId(), o-> log.debug("    -{}", o));
		printMap(requests, ses.getId(), r-> {
			log.debug("  -{}", r);
			printMap(stages, r.getId(), s-> log.debug("    -{}", s));
		});
	}
	
	static <T extends Metric> void printMap(Map<String, Set<T>> map, String key, Consumer<T> cons) {
		var stg = map.remove(key);
		if(nonNull(stg)) {
			stg.stream().sorted(METRIC_COMPARATOR).forEach(cons);
		}
	}
	
	static <T> void appendTrace(Map<String, Set<T>> map, String key, T element) {
		if(nonNull(key)) {
			map.compute(key, (k,v)->{
				if(v == null) {
					v = new LinkedHashSet<>();
				}
				v.add(element);
				return v;
			});
		}
		else { 
			new Exception("no active session/request " + element).printStackTrace();
		}
	}
}
