package org.usf.inspect.core;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Stream.generate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 
 * 
 * @author u$f
 *
 */
class EventTraceEmitterTest {
	
	private EventTraceEmitter emitter;
	
	@BeforeEach
	void clearHandlers() {
		emitter = new EventTraceEmitter();
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 5, 10, 20, 50, 100})
	void testRegister(int n) {
		nParallelExec(n, ()-> emitter.addHandler(s-> {}));
//		assertEquals(n, handlers.size());
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 5, 10, 20, 50, 100})
	void testEmit(int n) {
		var reg = generate(AtomicInteger::new).limit(10).toArray(AtomicInteger[]::new);
		for(var o : reg) {
			emitter.addHandler(s-> o.incrementAndGet());
		}
		nParallelExec(n, ()-> emitter.emitTrace(new RestSession()));
		for(var o : reg) {
			assertEquals(n, o.get());
		}
	}
	
	static void nParallelExec(int n, Runnable r) {
		var pool = newFixedThreadPool(n);
		try {
			allOf(generate(()-> runAsync(r, pool))
					.limit(n)
					.toArray(CompletableFuture[]::new))
			.join();
		}
		finally {
			pool.shutdown();
		}
	}
}
