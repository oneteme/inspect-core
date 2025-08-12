package org.usf.inspect.core;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

class ConcurrentLinkedSetQueueTest {
	
	private ConcurrentLinkedSetQueue<Dummy> queue;
	
	@BeforeEach
	void init() {
		queue = new ConcurrentLinkedSetQueue<>();
	}
	
	@Test
	void testAdd() {
		testQueue(true, arr-> stream(arr).forEach(queue::add));
	}

	@Test
	void testAddAll_collection() {
		testQueue(true, arr-> queue.addAll(asList(arr)));
	}

	@Test
	void testAdd_not_overwirte() {
		testQueue(false, arr-> queue.addAll(false, asList(arr)));
	}

	@Test
	void testPop_empty() {
		var c = queue.pop();
		assertTrue(c.isEmpty());
		assertNotSame(queue.queue, c);
	}

	@Test
	void testPop() {
		var arr = new Dummy[3];
		queue.add(arr[0] = new Dummy(123, "v1"));
		queue.add(arr[1] = new Dummy(123, "v2"));
		queue.add(arr[2] = new Dummy(321, "v1"));
		var c = queue.pop();
		assertTrue(queue.queue.isEmpty());
		assertInstanceOf(LinkedHashSet.class, c);
		assertEquals(2, c.size());
		var it = c.iterator();
		assertSame(arr[1], it.next());
		assertSame(arr[2], it.next());
	}
	
	@ParameterizedTest
	@CsvSource({"0, 3", "-1, 3", "2, 1", "3, 0", "10, 0"})
	void testRemoveFrom(int n, int size) {
		queue.add(new Dummy(123, "v1")); 
		queue.add(new Dummy(231, "v1"));
		queue.add(new Dummy(312, "v1"));
		assertEquals(size, queue.removeFrom(n));
	}

	void testQueue(boolean overwite, Consumer<Dummy[]> cons) {
		var o1 = new Dummy(123, "v1"); 
		var o2 = new Dummy(123, "v2");
		var o3 = new Dummy(321, "v1");
//		var o4 = new Dummy(321, "v2")
		assertEquals(1, queue.add(o1));
		cons.accept(new Dummy[] {o2, o3}); //add / addAll
		assertEquals(2, queue.size());
		var c = new ArrayList<>(queue.queue);
		assertSame(overwite ? o2 : o1, c.get(0));
		assertNotSame(overwite ? o1 : o2, c.get(0));
		assertSame(o3, c.get(1));
	}
	
	@Getter
	@ToString
	@RequiredArgsConstructor
	@EqualsAndHashCode(of = "id")
	static class Dummy {
		
		private final int id;
		private final String name;
	}
}
