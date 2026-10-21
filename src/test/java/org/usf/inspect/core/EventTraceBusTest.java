package org.usf.inspect.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.usf.inspect.InstanceEnvironment;
import org.usf.inspect.InstanceType;

/**
 * 
 * @author u$f
 *
 */
class EventTraceBusTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10})
    void should_trigger_instance_emit_on_registered_hook_with_same_environment(int hookCount) {
        var bus = new EventTraceBus();
        var arr = new ArrayList<InstanceEnvironment>();
        for(int i=0; i<hookCount; i++) {
			bus.registerHook(new DispatchHook() {
				@Override
				public void onInstanceEmit(InstanceEnvironment env) {
					arr.add(env);
				}
			});
		}
        var exp = newInstanceEnvironment();
        assertDoesNotThrow(() -> bus.triggerInstanceEmit(exp));
        assertEquals(hookCount, arr.size());
        for(int i=0; i<hookCount; i++) {
			assertSame(exp, arr.get(i));
		}
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10})
    void should_trigger_schedule_on_all_registered_hooks(int hookCount) {
        var bus = new EventTraceBus();
        var scheduleCount = new AtomicInteger();
        for(int i=0; i<hookCount; i++) {
			bus.registerHook(new DispatchHook() {
				@Override
				public void onSchedule() {
					scheduleCount.incrementAndGet();
				}
			});
		}
        assertDoesNotThrow(bus::triggerSchedule);
        assertEquals(hookCount, scheduleCount.get());
    }

    @Test
    void should_continue_instance_emit_when_a_hook_throws_exception() {
        var bus = new EventTraceBus();
        var received = new AtomicReference<InstanceEnvironment>();
        bus.registerHook(new DispatchHook() {
            @Override
            public void onInstanceEmit(InstanceEnvironment env) {
                throw new IllegalStateException("error");
            }
        });
        bus.registerHook(new DispatchHook() {
            @Override
            public void onInstanceEmit(InstanceEnvironment env) {
                received.set(env);
            }
        });
        var expected = newInstanceEnvironment();
        assertDoesNotThrow(() -> bus.triggerInstanceEmit(expected));
        assertSame(expected, received.get());
    }

    @Test
    void should_continue_schedule_when_a_hook_throws_exception() {
        var bus = new EventTraceBus();
        var scheduleCount = new AtomicInteger();
        bus.registerHook(new DispatchHook() {
            @Override
            public void onSchedule() {
                throw new IllegalStateException("error");
            }
        });
        bus.registerHook(new DispatchHook() {
            @Override
            public void onSchedule() {
                scheduleCount.incrementAndGet();
            }
        });
        assertDoesNotThrow(bus::triggerSchedule);
        assertEquals(1, scheduleCount.get());
    }

    private InstanceEnvironment newInstanceEnvironment() {
        return new InstanceEnvironment(
                UUID.randomUUID(),
                Instant.now(),
                InstanceType.SERVER,
                "inspect-core",
                "1.0.0",
                "test",
                "127.0.0.1",
                "Windows",
                "Java",
                "tester",
                "main",
                "abc123",
                "collector",
                Map.of(),
                null);
    }
}
