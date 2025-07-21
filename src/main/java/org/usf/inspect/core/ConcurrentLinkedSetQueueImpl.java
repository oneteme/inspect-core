package org.usf.inspect.core;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Stream.empty;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author u$f
 */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class ConcurrentLinkedSetQueueImpl<T> implements ConcurrentLinkedSetQueue<T> {

	private final Object mutex = new Object();
    private LinkedHashSet<T> queue;

    public ConcurrentLinkedSetQueueImpl() {
		this(new LinkedHashSet<>());
    }
    
    /**
	 * Adds an item to the queue, overwriting existing items (more recent).
	 */
	@Override
	public int add(T o) { //return size
    	synchronized(mutex){
			queue.add(o);
			return queue.size();
    	}
	}
	
	/**
	 * Adds all items to the queue, overwriting existing items (more recent).
	 */
	@Override
	public int addAll(Collection<T> arr){
    	synchronized(mutex){
			queue.addAll(arr); //add or overwrite items (update)
			return queue.size();
		}
	}
	
	/**
	 * Adds all items to the queue, overwriting existing items (more recent).
	 */
	@Override
	public int addAll(T[] arr){
    	synchronized(mutex){
    		Collections.addAll(queue, arr);
			return queue.size();
		}
	}
	
	/**
	 * Prepends items to the queue, preserving their order.
	 * If an item already exists in the queue, the existing (more recent) version is kept and the one from {@code arr} is ignored.
	 */
	@Override
	public void requeueAll(Collection<T> arr){
    	synchronized(mutex){
    		var set = new LinkedHashSet<>(arr);
    		set.addAll(queue); //add or overwrite items (update)
    		queue = set;
		}
	}
    
    /**
     * Pops all items from the queue, clearing it.
     */
    @Override
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
	
    @Override
	public Stream<T> peek() {
    	synchronized(mutex){
    		return queue.isEmpty() ? empty() : queue.stream();
    	}
    }
    
	@Override
	public int size() {
		synchronized (mutex) {
			return queue.size();
		}
	}
	
    @Override
	public int removeIf(Predicate<T> filter) {
    	synchronized(mutex){
    		var size = queue.size();
    		queue.removeIf(filter);
    		return size - queue.size();
    	}
    }
    
    @Override
	public int removeNLast(int n) { 
    	synchronized(mutex){ // queue.reversed().iterator : java21
    		var size = queue.size();
    		if(queue.size() > n) {
    			var it =  queue.iterator();
        		for(var i=queue.size()-n; i>0; i--, it.next());
        		while(it.hasNext()) {
        			it.next();
        			it.remove();
        		}
    		}
    		else if(n > 0) {
    			queue = new LinkedHashSet<>();
    		}
    		else {
    			throw new IllegalArgumentException("illegal parameter value=" + n);
    		}
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