package org.usf.traceapi.core;

import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.logSessions;

import java.util.List;

/**
 * 
 * @author u$f
 *
 */
public final class SessionLogger implements TraceHandler {
	
	private final ScheduledDispatcher<Session> dispatcher;
	
	public SessionLogger(TraceConfigurationProperties properties) {
		this.dispatcher = log.isDebugEnabled() 
				? new ScheduledDispatcher<>(properties, this::print, Session::completed)
				: null; //not init
	}
	
	@Override
	public void handle(Session session) {
		dispatcher.add(session);
	}
	
    private boolean print(int attemps, List<Session> sessions) {
		logSessions(sessions);
		return true;
    }
}
