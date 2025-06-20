package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.log;

import java.util.Collection;

/**
 * 
 * @author u$f
 *
 */
final class SessionLogger implements SessionHandler<Session> { //inspect.client.log : SESSION | REQUEST | STAGE

	@Override //sync. avoid session log collision
	public synchronized void handle(Session s) {
		if(log.isDebugEnabled()) {
			log.debug("+ {}", s);
			for(var req : s.getRestRequests()) {
				printSessionStage(req);
			}
			for(var req : s.getDatabaseRequests()) {
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
			for(var req : s.getLdapRequests()) {
				printSessionStage(req);
				printRequestStages(req.getActions());
			}
			for(var req : s.getLocalRequests()) {
				printSessionStage(req);
			}
		}
    }
	
	private static void printSessionStage(SessionStage stg) {
		log.debug("\t- {}", stg);
	}
	
	private static void printRequestStages(Collection<? extends RequestStage> stages) {
		if(log.isTraceEnabled()) {
			for(var stg : stages) {
				log.trace("\t\t- {}", stg);
			}	
		}
	}
}
