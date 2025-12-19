package org.usf.inspect.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.MailAction.CONNECTION;
import static org.usf.inspect.core.MailAction.DISCONNECTION;
import static org.usf.inspect.core.MailAction.EXECUTE;

import java.util.stream.Stream;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Mail;
import org.usf.inspect.core.MailAction;
import org.usf.inspect.core.MailCommand;
import org.usf.inspect.core.MailRequest2;
import org.usf.inspect.core.MailRequestCallback;
import org.usf.inspect.core.Monitor.StatefulMonitor;
import org.usf.inspect.core.SessionContextManager;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;

/**
 * 
 * @author u$f
 *
 */
final class MailRequestMonitor extends StatefulMonitor<MailRequest2, MailRequestCallback> {
	
	ExecutionListener<Object> handleConnection(Transport trsp) {
		return traceBegin(SessionContextManager::createMailRequest, (req,v)->{
			var url = trsp.getURLName();
			if(nonNull(url)) {
				req.setProtocol(url.getProtocol());
				req.setHost(url.getHost());
				req.setPort(url.getPort());
				req.setUser(url.getUsername());
			}
		}).then(stageHandler(CONNECTION, null, null)); //before end if thrw
	}
	
	//callback should be created before processing
	protected MailRequestCallback createCallback(MailRequest2 session) { 
		return session.createCallback();
	}

	ExecutionListener<Object> handleDisconnection() {
		return stageHandler(DISCONNECTION, null, null).then(traceEnd());
	}
	
	<T> ExecutionListener<T> executeStageHandler(MailCommand cmd, Message msg) {
		return stageHandler(EXECUTE, cmd, msg);
	}
	
	<T> ExecutionListener<T> stageHandler(MailAction action, MailCommand cmd, Message msg) {
		return traceStep((s,e,o,t)-> callback.createStage(action, s, e, t, cmd, createMailTrace(msg)));
	}
	
	static Mail createMailTrace(Message msg) throws MessagingException {
		if(nonNull(msg)) {
			var mail = new Mail();
			mail.setSubject(msg.getSubject());
			mail.setFrom(toStringArray(msg.getFrom()));
			mail.setRecipients(toStringArray(msg.getAllRecipients()));
			mail.setReplyTo(toStringArray(msg.getReplyTo()));
			mail.setContentType(msg.getContentType());
			mail.setSize(msg.getSize());
			return mail;
		}
		return null;
	}
	
	static String[] toStringArray(Address... address) {
		return isNull(address) || address.length == 0
			? null 
			: Stream.of(address).map(Address::toString).toArray(String[]::new);
	}
}
