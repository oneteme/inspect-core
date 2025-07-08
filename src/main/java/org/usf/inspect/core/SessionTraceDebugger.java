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
public final class SessionTraceDebugger implements TraceHandler<Traceable> { //inspect.client.log : SESSION | REQUEST | STAGE
	
	private static final Comparator<? super Metric> METRIC_COMPARATOR = comparing(Metric::getStart);
	
	private Map<String, AbstractSession> sessions = synchronizedMap(new HashMap<>());
	private Map<String, Set<AbstractRequest>> requests = synchronizedMap(new HashMap<>());
	private Map<String, Set<AbstractStage>> stages = synchronizedMap(new HashMap<>());

	@Override
	public void handle(Traceable t) {
		if(t instanceof AbstractSession s) {
			if(s.wasCompleted()) {
				printSession(s);
				return;
			}
			sessions.put(s.getId(), s);
		}
		else if(t instanceof AbstractRequest r) {
			appendTrace(requests, r.getSessionId(), r);
		}
		else if(t instanceof AbstractStage s) {
			appendTrace(stages, s.getRequestId(), s);
		}
    }
	
	void printSession(AbstractSession ses) {
		sessions.remove(ses.getId());
		log.debug(">{}",ses);
		printMap(stages, ses.getId(), o-> log.debug("\t\t-{}", o));
		printMap(requests, ses.getId(), r-> {
			log.debug("\t-{}", r);
			printMap(stages, r.getId(), s-> log.debug("\t\t-{}", s));
		});
	}
	
	<T extends Metric> void printMap(Map<String, Set<T>> map, String key, Consumer<T> cons) {
		var stg = map.remove(key);
		if(nonNull(stg)) {
			stg.stream().sorted(METRIC_COMPARATOR).forEach(cons);
		}
	}
	
	@Override
	public void complete() throws Exception {
		log.warn("unfinished tasks");
		sessions.values().forEach(this::printSession);
	}
	
	static <T> void appendTrace(Map<String, Set<T>> map, String key, T element) {
		map.compute(key, (k,v)->{
			if(v == null) {
				v = new LinkedHashSet<>();
			}
			v.add(element);
			return v;
		});
	}
}
