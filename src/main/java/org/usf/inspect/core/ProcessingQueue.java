package org.usf.inspect.core;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;

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
public final class ProcessingQueue<T> {

	private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>(); 
	
	public boolean add(T o) { //return size, reduce sync call
		return queue.add(o);
	}

	public boolean addAll(Collection<T> arr){
		return queue.addAll(arr);
	}
	
	public void pollAll(int max, UnaryOperator<List<T>> op) {
		List<T> items = new ArrayList<>();
		try {
			T obj;
			var idx = 0;
			while (idx++<max && nonNull(obj = queue.poll())) { 
		        items.add(obj);
		    }
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
	
	public List<T> peek() {
		return new ArrayList<>(queue);
	}

	public int size() {
		return queue.size();
	}

	@Override
	public String toString() {
		return queue.toString();
	}
}