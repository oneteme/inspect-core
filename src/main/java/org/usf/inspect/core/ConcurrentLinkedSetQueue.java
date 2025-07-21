package org.usf.inspect.core;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Stream.empty;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A thread-safe queue that allows adding items while maintaining uniqueness and order.
 * 
 * @param <T>
 * 
 * @author u$f
 */
public interface ConcurrentLinkedSetQueue<T> {

	/**
	 * Adds an item to the queue, overwriting existing items (more recent).
	 * 
	 * @return the size of the queue after adding the item
	 */
	int add(T o);

	/**
	 * Adds all items to the queue, overwriting existing items (more recent).
	 * 
	 * @return the size of the queue after adding the item
	 */
	int addAll(Collection<T> arr);

	/**
	 * Adds all items to the queue, overwriting existing items (more recent).
	 * 
	 * @return the size of the queue after adding the item
	 */
	int addAll(T[] arr);

	/**
	 * Prepends items to the queue, preserving their order.
	 * If an item already exists in the queue, the existing (more recent) version is kept and the one from {@code arr} is ignored.
	 */
	void requeueAll(Collection<T> arr);

	/**
	 * Pops all items from the queue, clearing it.
	 */
	Set<T> pop();

	Stream<T> peek();

	int size();

	int removeIf(Predicate<T> filter);

	int removeNLast(int n);

	
	public static <T> ConcurrentLinkedSetQueue<T> noQueue() {
		
		return new ConcurrentLinkedSetQueue<>() {
			
			@Override
			public int add(T o) {
				return 0; //do not add to empty queue
			}
			
			@Override
			public int addAll(Collection<T> arr) {
				return 0; //do not add to empty queue
			}
			
			@Override
			public int addAll(T[] arr) {
				return 0; //do not add to empty queue
			}
			
			@Override
			public void requeueAll(Collection<T> arr) {
				//do not requeue to empty queue
			}
			
			@Override
			public Set<T> pop() {
				return emptySet();
			}
			
			@Override
			public Stream<T> peek() {
				return empty();
			}
			
			@Override
			public int size() {
				return 0;
			}
			
			@Override
			public int removeIf(Predicate<T> filter) {
				return 0; //do not remove from empty queue
			}
			
			@Override
			public int removeNLast(int n) {
				return 0; //do not remove from empty queue
			}
			
			@Override
			public String toString() {
				return "[]";
			}
		};
	}
}