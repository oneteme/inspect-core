package org.usf.traceapi.core;

import static org.usf.traceapi.core.Helper.log;

import java.util.Collection;
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
		for(var s : sessions) {
			log.debug("~ {}", s);
			for(var req : s.getRequests()) {
				printSessionStage(req);
			}
			for(var req : s.getQueries()) {
				printSessionStage(req);
				printRequestStages(req.getActions());
			}
			for(var req : s.getFtpRequests()) {
				printSessionStage(req);
				printRequestStages(req.getActions());
			}
			for(var req : s.getMailRequests()) {
				printSessionStage(req);
				printRequestStages(req.getActions());
			}
			for(var req : s.getStages()) {
				printSessionStage(req);
			}
		}
		return true;
    }

	private static void printSessionStage(SessionStage stg) {
		log.debug("\t{}-", stg);
	}
	private static void printRequestStages(Collection<? extends RequestStage> stages) {
		for(var stg : stages) {
			log.debug("\t\t{}-", stg);
		}
	}
}
