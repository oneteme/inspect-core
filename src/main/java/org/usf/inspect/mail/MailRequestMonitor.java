package org.usf.inspect.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionManager.createMailRequest;
import static org.usf.inspect.mail.MailAction.CONNECTION;
import static org.usf.inspect.mail.MailAction.DISCONNECTION;

import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.Mail;
import org.usf.inspect.core.MailRequest;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class MailRequestMonitor {
	
	private final MailRequest req = createMailRequest();
	private final Transport trsp;

	public MailRequest handleConnection(Instant start, Instant end, Void v, Throwable thw) {
		context().emitTrace(req.createStage(CONNECTION, start, end, thw));
		req.setThreadName(threadName());
		req.setStart(start);
		if(nonNull(thw)) { // if connection error
			req.setEnd(end);
		}
		else {
			req.setMails(new ArrayList<>(1));
		}
		var url = trsp.getURLName();
		if(nonNull(url)) {
			req.setProtocol(url.getProtocol());
			req.setHost(url.getHost());
			req.setPort(url.getPort());
			req.setUser(url.getUsername());
		}
		return req;
	}

	public MailRequest handleDisconnection(Instant start, Instant end, Void v, Throwable thw) {
		context().emitTrace(req.createStage(DISCONNECTION, start, end, thw));
		req.runSynchronized(()-> req.setEnd(end));
		return req;
	}
	
	<T> ExecutionMonitorListener<T> stageHandler(MailAction action) {
		return (s,e,o,t)-> req.createStage(action, s, e, t);
	}
	
	void appendMail(Message arg0, Address[] arg1) {
		var mail = new Mail(); // broke request Mail dependency !?
		try {
			mail.setSubject(arg0.getSubject());
			mail.setFrom(toStringArray(arg0.getFrom()));
			mail.setRecipients(toStringArray(arg0.getAllRecipients()));
			mail.setReplyTo(toStringArray(arg0.getReplyTo()));
			mail.setContentType(arg0.getContentType());
			mail.setSize(arg0.getSize());
		}
		catch (Exception e) {
			context().reportEventHandleError("MailRequestMonitor.appendMail", req, e);
		}
		finally {
			req.getMails().add(mail);
		}
	}
	
	private static String[] toStringArray(Address... address) {
		return isNull(address) || address.length == 0
			? null 
			: Stream.of(address).map(Address::toString).toArray(String[]::new);
	}
}
