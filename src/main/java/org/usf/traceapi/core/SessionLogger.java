package org.usf.traceapi.core;

import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.Helper.log;

import java.util.Collection;
import java.util.List;

import org.usf.traceapi.core.ScheduledDispatcher.Dispatcher;

/**
 * 
 * @author u$f
 *
 */
public final class SessionLogger implements Dispatcher<Session> {
	
	@Override
    public boolean dispatch(int attemps, List<Session> sessions) {
		try {
			for(var s : sessions) {
				log.debug("| {}", s);
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
		}
		catch (Exception e) {
			log.warn("error while loggin sessions {}", e.getMessage());
		}
		return true; //important! always returns true
    }

	private static void printSessionStage(SessionStage stg) {
		log.debug("\t- {}", stg);
	}
	private static void printRequestStages(Collection<? extends RequestStage> stages) {
		for(var stg : stages) {
			log.debug("\t\t- {}", stg);
		}
	}
}
