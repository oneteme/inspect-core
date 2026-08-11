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
 * Thread-safe queue that holds event traces pending dispatch, with optional waste-mode to discard new additions.
 *
 * @param <T> the trace element type
 * @author u$f
 */
@Slf4j
public final class ProcessingQueue<T> {

	private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
	private boolean waste;
	
	/**
	 * Adds the given element to the queue unless the queue is in waste mode.
	 *
	 * @param o the element to add
	 * @return {@code true} if the element was added or the queue is in waste mode
	 */
	public boolean add(T o) { //return size, reduce sync call
		return waste || queue.add(o);
	}

	public boolean addAll(Collection<T> arr){
		return waste || queue.addAll(arr);
	}
	
	/**
	 * Drains the current snapshot of queue elements, applies the given operator, then adds unprocessed elements back.
	 *
	 * @param op the operator that processes the drained elements and returns those that were not consumed
	 */
	public void pollAll(UnaryOperator<List<T>> op) {
		List<T> items = new ArrayList<>();
		try {
			T obj;
			var idx = 0;
			var max = queue.size();
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
	
	/**
	 * Returns a snapshot of all elements currently in the queue without removing them.
	 *
	 * @return the snapshot list
	 */
	public List<T> peek() {
		return new ArrayList<>(queue);
	}

	/**
	 * Returns the number of elements currently held in the queue.
	 *
	 * @return the queue size
	 */
	public int size() {
		return queue.size();
	}

	@Override
	public String toString() {
		return queue.toString();
	}
	
	void setWaste(boolean waste) {
		this.waste = waste;
	}
}