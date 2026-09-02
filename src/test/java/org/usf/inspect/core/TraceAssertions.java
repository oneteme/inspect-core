package org.usf.inspect.core;

import static java.lang.Thread.currentThread;
import static java.time.Duration.between;
import static java.util.Objects.isNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraceAssertions {
	
	public static <T extends AbstractRemoteRequestSignal> T assertRequestSignal(String scheme, String host, int port, String user, Instant beforeStart, Class<T> type, EventTrace trace){
  		var sgn = assertInstanceOf(type, trace);
		assertRequestSignal(scheme, host, port, user, beforeStart, sgn);
		return sgn;
	}
	
	public static void assertRequestSignal(String scheme, String host, int port, String user, Instant beforeStart, AbstractRemoteRequestSignal signal){
		assertNotNull(signal.getId());
		assertNull(signal.getSessionId()); //no active session
		assertTrue(signal.getStart().compareTo(beforeStart) >= 0);
		assertEquals(currentThread().getName(), signal.getThreadName()); 
		
		assertEquals(scheme, signal.getProtocol());
		assertEquals(host, signal.getHost());
		assertEquals(port, signal.getPort());
		assertEquals(user, signal.getUser());
	}
	
	public static <T extends AbstractStage> T assertRequestStage(String action, String cmd, UUID reqId, int offset, StagePayload ps, Instant prvStageEnd, Class<T> type, EventTrace trace){
		var stg = assertInstanceOf(type, trace);
		assertRequestStage(action, cmd, reqId, offset, ps, prvStageEnd, stg);
		return stg;
	}
	
	public static void assertRequestStage(String action, String cmd, UUID reqId, int offset, StagePayload ps, Instant prvStageEnd, AbstractStage stage){
		assertEquals(action, stage.getName());
		assertTrue(stage.getStart().compareTo(prvStageEnd) >= 0);
		assertTrue(stage.getEnd().compareTo(stage.getStart()) >= 0);
		assertEquals(cmd, stage.getCommand());
		assertEquals(reqId, stage.getRequestId());
		assertEquals(offset, stage.getOrder());
		if(isNull(ps)) {
			assertNull(stage.getPayload());
		}
		else {
			assertNotNull(stage.getPayload());
			assertArrayEquals(ps.getArgs(), stage.getPayload().getArgs());
			assertArrayEquals(ps.getCount(), stage.getPayload().getCount());
		}
	}
	
	public static ExceptionTrace assertExceptionTrace(UUID id, int offset, EventTrace trace) {
		var exp = assertInstanceOf(ExceptionTrace.class, trace);
		assertExceptionTrace(id, offset, exp);
		return exp;
	}

	public static void assertExceptionTrace(UUID id, int offset, ExceptionTrace ex) {
		assertEquals(ex.getTraceId(), id);
		assertEquals(offset, ex.getOffset());
		assertNotNull(ex.getType()); //assert exception type ?
		assertNotNull(ex.getMessage()); //assert exception msg ?
		assertNull(ex.getStackTraceRows());
		assertNull(ex.getCause());
	}
	
	public static <T extends AbstractRequestUpdate> T assertRequestUpdate(int status, UUID id, String command, Instant lastStageEnd, Instant afterEnd, Class<T> type, EventTrace trace){
		var upd = assertInstanceOf(type, trace);
		assertRequestUpdate(status, id, command, lastStageEnd, afterEnd, upd);
		return upd;
	}
	
	public static void assertRequestUpdate(int status, UUID id, String command, Instant lastStageEnd, Instant afterEnd, AbstractRequestUpdate update){
		assertEquals(id, update.getId());
		assertEquals(command, update.getCommand());
		assertEquals(status, update.getStatus());
		assertTrue(update.getEnd().compareTo(lastStageEnd) >= 0);
		assertTrue(update.getEnd().compareTo(afterEnd) <= 0);
	}
	
	public static long performance(List<EventTrace> trace) {
		assertTrue(trace.size() >= 2);
		var sgn = assertInstanceOf(AbstractRequestSignal.class, trace.get(0));
		var elp = 0;
		for(var i=1; i<trace.size()-1; i++) {
			var trc = trace.get(i);
			if(trc instanceof AbstractStage stg) {
				elp += between(stg.getStart(), stg.getEnd()).toNanos();
			}
		}
		var upd = assertInstanceOf(AbstractRequestUpdate.class, trace.get(trace.size()-1));
		return between(sgn.getStart(), upd.getEnd()).toNanos() - elp;
	}
}
