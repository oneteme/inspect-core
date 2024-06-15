package org.usf.traceapi.core;

import static org.usf.traceapi.core.Helper.log;

import java.util.Collection;

public class SessionPrettyLogger implements TraceHandler {

	@Override
	public void handle(Session session) {
		log.info("{}", session);
		for(var req : session.getRequests()) {
			printSessionStage(req);
		}
		for(var req : session.getQueries()) {
			printSessionStage(req);
			printStages(req.getActions());
		}
		for(var req : session.getFtpRequests()) {
			printSessionStage(req);
			printStages(req.getActions());
		}
		for(var req : session.getMailRequests()) {
			printSessionStage(req);
			printStages(req.getActions());
		}
		for(var req : session.getStages()) {
			printSessionStage(req);
		}
	}

	private void printSessionStage(SessionStage stg) {
		log.info("\t{}", stg);
	}
	private void printStages(Collection<? extends RequestStage> stages) {
		for(var stg : stages) {
			log.info("\t\t{}", stg);
		}
	}

}
