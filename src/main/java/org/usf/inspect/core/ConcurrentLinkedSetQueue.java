package org.usf.inspect.core;

import static java.util.Collections.emptyList;
import static java.util.stream.Stream.empty;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ConcurrentLinkedSetQueue<T> implements EventHandler<T> {

	private final Object mutex = new Object();
    private LinkedHashSet<T> queue = new LinkedHashSet<>(); //guarantees order and uniqueness of items, no duplicates (force updates)

    @Override
    public void handle(T obj) throws Exception {
    	add(obj);
    }

    /**
	 * Adds an item to the queue, overwriting existing items (more recent).
	 */
	public void add(T o) {
    	synchronized(mutex){
			queue.add(o);
			logAddedItems(1, queue.size());
    	}
	}
	
	/**
	 * Adds all items to the queue, overwriting existing items (more recent).
	 */
	public void addAll(Collection<T> arr){
    	synchronized(mutex){
			queue.addAll(arr); //add or overwrite items (update)
			logAddedItems(arr.size(), queue.size());
		}
	}
	
	/**
	 * Prepends items to the queue, preserving their order.
	 * If an item already exists in the queue, the existing (more recent) version is kept and the one from {@code arr} is ignored.
	 */
	public void requeueAll(Collection<T> arr){
    	synchronized(mutex){
    		var set = new LinkedHashSet<>(arr);
    		set.addAll(queue); //add or overwrite items (update)
    		queue = set;
			logAddedItems(arr.size(), queue.size());
		}
	}
	
    public Stream<T> peek() {
    	synchronized(mutex){
    		return queue.isEmpty() ? empty() : queue.stream();
    	}
    }
    
    /**
     * Pops all items from the queue, clearing it.
     */
    public Collection<T> pop() {
    	synchronized(mutex){
    		if(queue.isEmpty()) {
    			return emptyList();
    		}
    		var res = queue;
			queue = new LinkedHashSet<>(); //reset queue, may release memory (do not use clear())
			return res;
    	}
    }
    
	public int size() {
		synchronized (mutex) {
			return queue.size();
		}
	}
	
    static void logAddedItems(int nItems, int queueSize) {
		log.trace("{} items added or requeued to the queue (may overwrite existing), current queue size: {}", nItems, queueSize);
    }
}