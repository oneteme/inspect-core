package org.usf.inspect.core;

import static java.util.Collections.synchronizedMap;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;

import java.util.HashMap;
import java.util.HashSet;
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
public final class SessionDebugger implements SessionHandler<Traceable> { //inspect.client.log : SESSION | REQUEST | STAGE
	
	private Map<String, AbstractSession> sessions = synchronizedMap(new HashMap<>());
	private Map<String, Set<AbstractRequest<?>>> requests = synchronizedMap(new HashMap<>());
	private Map<String, Set<AbstractStage>> stages = synchronizedMap(new HashMap<>());

	@Override //sync. avoid session log collision
	public void handle(Traceable t) {
		if(t instanceof AbstractSession s) {
			if(s.wasCompleted()) {
				sessions.remove(s.getId());
				printSession(s);
				return;
			}
			sessions.put(s.getId(), s);
		}
		else if(t instanceof AbstractRequest<?> r) {
			requests.compute(r.getSessionId(), (k,v)->{
				if(v == null) {
					v = new LinkedHashSet<>();
				}
				v.add(r);
				return v;
			});
		}
		else if(t instanceof AbstractStage s) {
			stages.compute(s.getRequestId(), (k,v)->{
				if(v == null) {
					v = new LinkedHashSet<>();
				}
				v.add(s);
				return v;
			});
		}
    }
	
	void printSession(AbstractSession ses) {
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.err.println(ses);
		printMap(stages, ses.getId(), o-> System.err.println("\t\t-" + o));
		printMap(requests, ses.getId(), r->{
			System.err.println("\t-" + r);
			printMap(stages, r.getId(), s-> System.err.println("\t\t-" + s));
		});
		System.out.println();
	}
	
	<T extends Metric> void printMap(Map<String, Set<T>> map, String key, Consumer<T> cons) {
		var stg = map.remove(key);
		if(nonNull(stg)) {
			stg.stream().sorted(comparing(Metric::getStart)).forEach(cons);
		}
	}
	
	@Override
	public void complete() throws Exception {
		
	}
}
