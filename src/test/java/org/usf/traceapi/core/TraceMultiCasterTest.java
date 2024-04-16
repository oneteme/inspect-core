package org.usf.traceapi.core;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.traceapi.core.TraceMultiCaster.emit;
import static org.usf.traceapi.core.TraceMultiCaster.handlers;
import static org.usf.traceapi.core.TraceMultiCaster.register;

import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 
 * 
 * @author u$f
 *
 */
class TraceMultiCasterTest {
	
	@BeforeEach
	void clearHandlers() {
		handlers.clear();
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 5, 10, 20, 50, 100})
	void testRegister(int n) {
		var service = newFixedThreadPool(n);
		var futures = range(0, n)
		.mapToObj(i-> runAsync(()-> register(s-> {}), service))
		.toArray(CompletableFuture[]::new);
		service.shutdown();
		assertDoesNotThrow(()-> allOf(futures).get());
		assertEquals(n, handlers.size());
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 5, 10, 20, 50, 100})
	void testEmit(int n) {
		var arr = new int[10];
		range(0, arr.length).forEach(i-> register(s-> {++arr[i];}));
		var service = newFixedThreadPool(n);
		var futures = range(0, n)
		.mapToObj(i-> runAsync(()-> emit(new ApiSession()), service))
		.toArray(CompletableFuture[]::new);
		service.shutdown();
		assertDoesNotThrow(()-> allOf(futures).get());
		IntStream.of(arr).forEach(s-> assertEquals(n, s));
	}
}
