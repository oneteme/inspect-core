package org.usf.traceapi.core;

import static org.usf.traceapi.core.Helper.log;

import java.util.Collection;

/**
 * 
 * @author u$f
 *
 */
public final class SessionLogger implements SessionHandler<Session> {

	@Override
	public void handle(Session s) {
		log.debug("+ {}", s);
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
	private static void printSessionStage(SessionStage stg) {
		log.debug("\t- {}", stg);
	}
	
	private static void printRequestStages(Collection<? extends RequestStage> stages) {
		for(var stg : stages) {
			log.trace("\t\t- {}", stg);
		}
	}
}
