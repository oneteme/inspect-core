package org.usf.inspect.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.createMailRequest;
import static org.usf.inspect.mail.MailAction.CONNECTION;
import static org.usf.inspect.mail.MailAction.DISCONNECTION;

import java.time.Instant;
import java.util.stream.Stream;

import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
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
		req.createStage(CONNECTION, start, end, thw).emit();
		req.setThreadName(threadName());
		req.setStart(start);
		if(nonNull(thw)) { // if connection error
			req.setEnd(end);
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
		req.createStage(DISCONNECTION, start, end, thw).emit();
		req.runSynchronized(()-> req.setEnd(end));
		return req;
	}
	
	<T> ExecutionHandler<T> stageHandler(MailAction action, Message msg) {
		return (s,e,o,t)-> {
			var stg = req.createStage(action, s, e, t);
			if(nonNull(msg)) {
				var mail = new Mail();
				mail.setSubject(msg.getSubject());
				mail.setFrom(toStringArray(msg.getFrom()));
				mail.setRecipients(toStringArray(msg.getAllRecipients()));
				mail.setReplyTo(toStringArray(msg.getReplyTo()));
				mail.setContentType(msg.getContentType());
				mail.setSize(msg.getSize());
				stg.setMail(mail);
			}
			return stg;
		};
	}
	
	private static String[] toStringArray(Address... address) {
		return isNull(address) || address.length == 0
			? null 
			: Stream.of(address).map(Address::toString).toArray(String[]::new);
	}
}
