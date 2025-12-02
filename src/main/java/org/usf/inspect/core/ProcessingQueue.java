package org.usf.inspect.core;

import static java.lang.Integer.compare;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class ProcessingQueue {

	private final ConcurrentLinkedQueue<EventTrace> queue = new ConcurrentLinkedQueue<>(); //possible duplicates 
	
	public int add(EventTrace o) { //return size, reduce sync call
		queue.add(o);
		return queue.size();
	}

	public int addAll(Collection<EventTrace> arr){
		queue.addAll(arr); 
		return queue.size();
	}
	
	public void pollAll(int max, UnaryOperator<List<EventTrace>> op) {
		List<EventTrace> items = new ArrayList<>();
		try {
			EventTrace obj;
			var idx = 0;
			while (idx++<max && nonNull(obj = queue.poll())) { 
		        items.add(obj);
		    }
			resolveTraceUpdates(items);
			items = op.apply(items); //partial consumption, may return unprocessed items
		}
		catch (OutOfMemoryError e) {
			items = emptyList(); //do not add items back to the queue, may release memory
			log.error("out of memory error while queue processing, {} traces will be aborted", items.size());
			throw e;
		}
		finally {
			if(nonNull(items) && !items.isEmpty()) {
				queue.addAll(items);
			}
		}
	}

	public List<EventTrace> peek() {
		return new ArrayList<>(queue);
	}

	public int size() {
		return queue.size();
	}

	public int removeIfInstanceOf(Class<?> cls) {
		queue.removeIf(cls::isInstance);
		return queue.size();
	}

	@Override
	public String toString() {
		return queue.toString();
	}

	static void resolveTraceUpdates(List<EventTrace> traces){
		traces.stream()
			.filter(AbstractSessionCallback.class::isInstance)
			.map(AbstractSessionCallback.class::cast)
			.forEach(ses-> traces.removeIf(evn-> {
				if(evn instanceof SessionMaskUpdate upd && upd.getId().equals(ses.getId())) {
					ses.getRequestMask().updateAndGet(v-> v > upd.getMask() ? v : v | upd.getMask()); //server side : upd.getMask() may be > ses.getRequestMask()
					return true;
				}
				return false;
			}));
		traces.stream()
		.filter(SessionMaskUpdate.class::isInstance)
		.map(SessionMaskUpdate.class::cast)
		.collect(groupingBy(SessionMaskUpdate::getId))
		.values().forEach(v-> v.stream().reduce((prv, cur)-> {
				if(prv.getMask() > cur.getMask()) {
					traces.remove(cur);
					return prv;
				}
				else {
					traces.remove(prv);
					return cur;
				}
			}));
	}
}