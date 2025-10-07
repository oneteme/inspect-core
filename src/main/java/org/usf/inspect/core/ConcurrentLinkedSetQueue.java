package org.usf.inspect.core;

import static java.util.Collections.emptySet;
import static java.util.stream.Stream.empty;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author u$f
 */
public final class ConcurrentLinkedSetQueue<T> {

	private final Object mutex = new Object();
	LinkedHashSet<T> queue = new LinkedHashSet<>();
	
	public int add(T o) { //return size, reduce sync call
		return add(o, true);
	}

	public int add(T o, boolean overwrite) { //return size, reduce sync call
		synchronized(mutex){
			if(overwrite) {
				queue.remove(o);
			}
			queue.add(o); 
			return queue.size();
		}
	}

	public int addAll(Collection<T> arr){ //return size, reduce sync call
		return addAll(arr, true);
	}

	public int addAll(Collection<T> arr, boolean overwrite){
		synchronized(mutex){
			if(overwrite) {
				queue.removeAll(arr); // in order to add them even if exists
			}
			queue.addAll(arr); 
			return queue.size();
		}
	}

	public Set<T> pop() {
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
			} // else do nothing
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