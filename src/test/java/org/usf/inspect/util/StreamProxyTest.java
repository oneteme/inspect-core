package org.usf.inspect.util;

import static java.lang.Thread.currentThread;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.function.Predicate.not;
import static java.util.stream.Stream.generate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.util.StreamProxy.parallel;
import static org.usf.inspect.util.StreamProxy.trackStream;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.usf.inspect.AbstractSessionUpdate;
import org.usf.inspect.MainSessionUpdate;
import org.usf.inspect.core.SessionContext;
import org.usf.inspect.core.TestTraceHub;

@ExtendWith(TestTraceHub.class)
class StreamProxyTest {
	
	AbstractSessionUpdate session;

	@BeforeEach
	void setup(){
		session = new MainSessionUpdate(randomUUID());
		setActiveContext(session);
	}
	
	@AfterEach
	void tearDown() {
		clearContext(session);
	}

	@Test
	void testParallel_TryAdvance() {
		var tn = currentThread().getName();
		var sess = createParallelStream(100)
		.filter(not(v-> currentThread().getName().equals(tn))) //at 
		.findAny().map(v-> activeContext());
		assertTrue(sess.isPresent(), "Session should be present");
		assertEquals(session, sess.get().getSession(), "Session should be the same as the one before processing the stream");
	}

	@Test
	void testParallel_forEachRemaining() {
		var map = new ConcurrentHashMap<String, AbstractSessionUpdate>();
		createParallelStream(100)
		.forEach(v-> map.computeIfAbsent(currentThread().getName(), k-> ofNullable(activeContext()).map(SessionContext::getSession).orElse(null)));
		assertTrue(map.size() > 1, "Should have more than one thread");
		map.values().forEach(sess-> assertEquals(session, sess, "Session should be the same as the one before processing the stream"));
	}
	
	@Test
	@DisabledConfiguration
	void testTrackStream_TryAdvance() {
		var stream = createParallelStream(10);
		assertSame(stream, trackStream(stream), "Stream should be the same when tracking is disabled");
	}
	
	Stream<?> createParallelStream(int length) {
		return parallel(generate(()-> 0).limit(length));
	}
}
