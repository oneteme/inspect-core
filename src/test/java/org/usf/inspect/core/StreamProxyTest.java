package org.usf.inspect.core;

import static java.lang.Thread.currentThread;
import static java.util.UUID.randomUUID;
import static java.util.function.Predicate.not;
import static java.util.stream.Stream.generate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.StreamProxy.parallel;
import static org.usf.inspect.core.TestTraceHub.clearTraces;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestTraceHub.class)
class StreamProxyTest {
	
	private AbstractSessionUpdate ctx;

	@BeforeEach
	void setup(){
		clearTraces();
		ctx = new MainSessionUpdate(randomUUID());
		setActiveContext(ctx);
	}
	
	@AfterEach
	void tearDown() {
		clearContext(ctx);
	}

	@Test
	void testParallel_TryAdvance() {
		var tn = currentThread().getName();
		var ses = createParallelStream(100)
		.filter(not(v-> currentThread().getName().equals(tn)))
		.findAny().map(v-> activeContext());
		assertTrue(ses.isPresent(), "Session should be present");
		assertEquals(ctx, ses.get(), "Session should be the same as the one before processing the stream");
	}

	@Test
	void testParallel_forEachRemaining() {
		var map = new ConcurrentHashMap<String, AbstractSessionUpdate>();
		createParallelStream(100)
		.forEach(v-> map.computeIfAbsent(currentThread().getName(), k-> activeContext()));
		assertTrue(map.size() > 1, "Should have more than one thread");
		map.values().forEach(ses-> assertEquals(ctx, ses, "Session should be the same as the one before processing the stream"));
	}
	
	Stream<?> createParallelStream(int length) {
		return parallel(generate(()-> 0).limit(length));
	}
}
