package org.usf.inspect.core;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import io.micrometer.common.lang.NonNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class ProcessingQueue<T> {

	@Getter
	private final int maxCapacity;
	private final Queue<T> queue = new ConcurrentLinkedQueue<>();
	
	public boolean add(T o) {
		return queue.add(o);
	}

	public boolean addAll(Collection<T> arr){
		return queue.addAll(arr);
	}
	
	public void pollAll(@NonNull UnaryOperator<List<T>> op) {
		if(queue.isEmpty()) {
			op.apply(emptyList());
			return;
		}
		List<T> items = new ArrayList<>();
		try {
			T obj;
			var idx = 0;
			var max = queue.size();
			while(idx++<max && nonNull(obj = queue.poll())) { 
		        items.add(obj);
		    }
			items = op.apply(items); //partial consumption, may return unprocessed items
		}
		catch (OutOfMemoryError e) {
			log.error("out of memory error while queue processing, {} traces will be aborted", items.size());
			items = emptyList(); //do not add items back to the queue, may release memory
			throw e;
		}
		finally {
			if(nonNull(items) && !items.isEmpty()) {
				queue.addAll(items);
			}
		}
	}

	public int size() {
		return queue.size();
	}
	
	public boolean isCapacityExceeded() {
		return queue.size() > maxCapacity;
	}

	public Collection<T> peek() {
		return unmodifiableCollection(queue);
	}
	
	void clear() {
		queue.clear();
	}
	
	int removeIf(Predicate<? super T> pre) {
		var len = new AtomicInteger();
		queue.removeIf(t->{
			if(pre.test(t)) {
				len.incrementAndGet();
				return true;
			}
			return false;
		});
		return len.get();
	}
	
	@Override
	public String toString() {
		return queue.toString();
	}
}