package org.usf.inspect.core;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.UnaryOperator;

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
		.filter(SessionMaskUpdate.class::isInstance)
		.map(SessionMaskUpdate.class::cast)
		.collect(groupingBy(SessionMaskUpdate::getId))
		.values().forEach(c-> c.stream().reduce((prv, cur)-> { //keep the highest mask
				if(prv.getMask() > cur.getMask()) {
					traces.remove(cur);
					return prv;
				}
				else {
					traces.remove(prv);
					return cur;
				}
			}).ifPresent(mdk-> traces.stream()
				.filter(t-> t instanceof AbstractSessionCallback ses && mdk.getId().equals(ses.getId()))
				.map(AbstractSessionCallback.class::cast)
				.findFirst().ifPresent(ses-> {
					ses.getRequestMask().updateAndGet(v-> v > mdk.getMask() ? v : v | mdk.getMask());
					traces.remove(mdk); //remove mask if callback found
				})));
	}
}