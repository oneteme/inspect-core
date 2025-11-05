package org.usf.inspect.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.MailAction.CONNECTION;
import static org.usf.inspect.core.MailAction.DISCONNECTION;
import static org.usf.inspect.core.MailAction.EXECUTE;
import static org.usf.inspect.core.SessionManager.createMailRequest;

import java.time.Instant;
import java.util.stream.Stream;

import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.Mail;
import org.usf.inspect.core.MailCommand;
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

	public void handleConnection(Instant start, Instant end, Void v, Throwable thw) {
		req.createStage(CONNECTION, start, end, thw, null).emit(); //before end if thrw
		req.setStart(start);
		req.setThreadName(threadName());
		var url = trsp.getURLName();
		if(nonNull(url)) {
			req.setProtocol(url.getProtocol());
			req.setHost(url.getHost());
			req.setPort(url.getPort());
			req.setUser(url.getUsername());
		}
		if(nonNull(thw)) { // if connection error
			req.setEnd(end);
		}
		req.emit();
	}

	public void handleDisconnection(Instant start, Instant end, Void v, Throwable thw) {
		req.createStage(DISCONNECTION, start, end, thw, null).emit();
		req.runSynchronized(()-> req.setEnd(end));
	}
	
	<T> ExecutionHandler<T> executeStageHandler(MailCommand cmd, Message msg) {
		return (s,e,o,t)-> {
			Mail mail = null;
			if(nonNull(msg)) {
				mail = new Mail();
				mail.setSubject(msg.getSubject());
				mail.setFrom(toStringArray(msg.getFrom()));
				mail.setRecipients(toStringArray(msg.getAllRecipients()));
				mail.setReplyTo(toStringArray(msg.getReplyTo()));
				mail.setContentType(msg.getContentType());
				mail.setSize(msg.getSize());
			}
			req.createStage(EXECUTE, s, e, t, cmd, mail).emit();
		};
	}
	
	private static String[] toStringArray(Address... address) {
		return isNull(address) || address.length == 0
			? null 
			: Stream.of(address).map(Address::toString).toArray(String[]::new);
	}
}
