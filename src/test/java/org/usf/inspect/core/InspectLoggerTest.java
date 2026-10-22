package org.usf.inspect.core;

import static java.lang.Thread.currentThread;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.usf.inspect.SessionEventMask.EVENT;
import static org.usf.inspect.core.InspectLogger.emitAlert;
import static org.usf.inspect.core.InspectLogger.emitError;
import static org.usf.inspect.core.InspectLogger.emitInfo;
import static org.usf.inspect.core.InspectLogger.emitWarn;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.TestTraceHub.getTraces;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.usf.inspect.MainSessionUpdate;
import org.usf.inspect.SessionEvent;
import org.usf.inspect.SessionMaskUpdate;

/**
 * 
 * @author u$f
 *
 */
@ExtendWith(TestTraceHub.class)
class InspectLoggerTest {
	
	private MainSessionUpdate session;

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
    void should_emit_info_log_as_session_event() {
        emitInfo("info-message");
        assertSessionEvent("INFO", "info-message");
    }

    @Test
    void should_emit_warn_log_as_session_event() {
        emitWarn("warn-message");
        assertSessionEvent("WARN", "warn-message");
    }

    @Test
    void should_emit_error_log_as_session_event() {
        emitError("error-message");
        assertSessionEvent("ERROR", "error-message");
    }

    @Test
    void should_emit_alert_log_as_session_event() {
        emitAlert("alert-message");
        assertSessionEvent("ALERT", "alert-message");
    }

	public void assertSessionEvent(String type, String value) {
		assertEquals(2, getTraces().size());
		
		var upd = assertInstanceOf(SessionMaskUpdate.class, getTraces().get(0));
		assertEquals(EVENT.getValue(), upd.getMask());
		assertEquals(true, upd.isMain());
		assertSame(session.getId(), upd.getId());

		var evn = assertInstanceOf(SessionEvent.class, getTraces().get(1));
		assertEquals(type, evn.getType());
		assertEquals(value, evn.getValue());
		assertEquals(currentThread().getName(), evn.getScope());
		assertNull(evn.getLocation());
		assertSame(session.getId(), evn.getSessionId());
	}
    
}
