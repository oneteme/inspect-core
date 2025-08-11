package org.usf.inspect.core;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Stream.empty;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

/**
 * @author u$f
 */
public final class ConcurrentLinkedSetQueue<T> {

	private final Object mutex = new Object();
	LinkedHashSet<T> queue = new LinkedHashSet<>();

	/**
	 * Adds an item to the queue, overwriting existing items (more recent).
	 */
	public int add(T o) { //return size, reduce sync call
		synchronized(mutex){
			queue.remove(o);
			queue.add(o); 
			return queue.size();
		}
	}

	/**
	 * Adds all items to the queue, overwriting existing items (more recent).
	 */
	public int addAll(Collection<T> arr){ //return size, reduce sync call
		synchronized(mutex){
			queue.removeAll(arr);
			queue.addAll(arr); //add or overwrite items (update)
			return queue.size();
		}
	}

	/**
	 * Adds all items to the queue, overwriting existing items (more recent).
	 */
	public int addAll(T[] arr){  //return size, reduce sync call
		synchronized(mutex){
			return addAll(asList(arr));
		}
	}

	/**
	 * Prepends items to the queue, preserving their order.
	 * If an item already exists in the queue, the existing (more recent) version is kept and the one from {@code arr} is ignored.
	 */
	public void requeueAll(Collection<T> arr){
		synchronized(mutex){
			queue.addAll(arr); //!overwrite items (update)
		}
	}

	/**
	 * Pops all items from the queue, clearing it.
	 */
	public Collection<T> pop() {
		synchronized(mutex){
			if(queue.isEmpty()) {
				return emptySet();
			}
			var res = queue;
			queue = new LinkedHashSet<>(); //reset queue, may release memory (do not use clear())
			return res;
		}
	}

	public Stream<T> peek() {
		synchronized(mutex){
			return queue.isEmpty() ? empty() : queue.stream();
		}
	}

	public int size() {
		synchronized (mutex) {
			return queue.size();
		}
	}

	public int removeFrom(int n) { 
		synchronized(mutex){ // queue.reversed().iterator : java21
			var size = queue.size();
			if(n > 0 && n < size) {
				var it = queue.iterator(); //clear
				for(var i=0; i<n; i++, it.next());
				while(it.hasNext()) {
					it.next();
					it.remove();
				}
			}
			else if(n <= 0) { //avoid throwing exception
				queue = new LinkedHashSet<>();
			}// else do nothing
			return size - queue.size();
		}
	}

	@Override
	public String toString() {
		synchronized (mutex) {
			return queue.toString();
		}
	}
}