package org.usf.traceapi.core;

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
    public boolean dispatch(int attempts, List<Session> sessions) {
		if(attempts == 1) { //only first attempt
			try {
				for(var s : sessions) {
					printSession(s);
				}
			}
			catch (Exception e) {
				log.warn("error while loggin sessions {}", e.getMessage());
			}
		} // else !log
		return true; //important! always returns true
    }
	
	private static void printSession(Session s) {
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
			log.debug("\t\t- {}", stg);
		}
	}
}
