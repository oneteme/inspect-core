package org.usf.inspect.util;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.util.ExecutorServiceWrapper.wrap;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.usf.inspect.AbstractSessionUpdate;
import org.usf.inspect.MainSessionUpdate;
import org.usf.inspect.core.DisabledConfiguration;
import org.usf.inspect.core.SessionContext;
import org.usf.inspect.core.TestTraceHub;

/**
 * 
 * @author u$f
 *
 */
@ExtendWith(TestTraceHub.class)
class ExecutorServiceWrapperTest {

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
	void testSubmit_callable(){
		var wrapper = wrap(newSingleThreadExecutor());
		var ftr = wrapper.submit(() -> activeContext());
		var ctx = assertDoesNotThrow(()-> ftr.get(), "Failed to get context from future");
		assertSessionContext(ctx);
	}
	
	@Test
	void testSubmit_runnable(){
		var ctx = new AtomicReference<SessionContext>();
		var wrapper = wrap(newSingleThreadExecutor());
		var ftr = wrapper.submit(()-> { ctx.set(activeContext()); });
		assertDoesNotThrow(()-> ftr.get(), "Failed to get context from future");
		assertSessionContext(ctx.get());
	}
	
	@Test
	void testSubmit_runnable_with_result(){
		var ctx = new AtomicReference<SessionContext>();
		var wrapper = wrap(newSingleThreadExecutor());
		var ftr = wrapper.submit(()-> { ctx.set(activeContext()); }, "");
		assertDoesNotThrow(()-> ftr.get(), "Failed to get context from future");
		assertSessionContext(ctx.get());
	}
	
	@Test
	void testExecute() throws InterruptedException{
		var ctx = new AtomicReference<SessionContext>();
		var wrapper = wrap(newSingleThreadExecutor());
		assertDoesNotThrow(()-> wrapper.execute(()-> { ctx.set(activeContext()); }), "Failed to execute runnable");
		try {
			Thread.sleep(5); // wait for the runnable to complete
		}
		finally {
			assertSessionContext(ctx.get());
		}
	}
	
	void assertSessionContext(SessionContext ctx) {
		assertNotNull(ctx, "Context should not be null in the future");
		assertSame(ctx.getSession(), session, "Context should be different in the future");
	}

	@Test
	void testWrap() {
		var es = newSingleThreadExecutor();
		var wrapper = wrap(es);
		assertNotSame(es, wrapper, "Wrapped executor service should not be the same as the original");
		assertSame(es, ((ExecutorServiceWrapper)wrapper).es, "Wrapped executor service should delegate to the original executor service"); 
	}

	@Test
	void testWrap_null() {
		assertNull(wrap(null), "Wrapping a null executor service should return null");
	}
	
	@Test
	void testWrap_wrapper() {
		var es = wrap(newSingleThreadExecutor());
		assertSame(es, wrap(es), "Wrapping an already wrapped executor service should return the same instance");
	}
	
	@Test
	@DisabledConfiguration
	void test_disabled() {
		var es = newSingleThreadExecutor();
		assertSame(es, wrap(es), "Wrapping an executor service when configuration is disabled should return the same instance");
	}
}
