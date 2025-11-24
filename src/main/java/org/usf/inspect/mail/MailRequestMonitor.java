package org.usf.inspect.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.MailAction.CONNECTION;
import static org.usf.inspect.core.MailAction.DISCONNECTION;
import static org.usf.inspect.core.MailAction.EXECUTE;
import static org.usf.inspect.core.SessionManager.createMailRequest;

import java.time.Instant;
import java.util.stream.Stream;

import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.Mail;
import org.usf.inspect.core.MailCommand;
import org.usf.inspect.core.MailRequestCallback;

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
	
	private MailRequestCallback callback;
	private final Transport trsp;

	public void handleConnection(Instant start, Instant end, Void v, Throwable thw) {
		var req = createMailRequest(start);
		var url = trsp.getURLName();
		if(nonNull(url)) {
			req.setProtocol(url.getProtocol());
			req.setHost(url.getHost());
			req.setPort(url.getPort());
			req.setUser(url.getUsername());
		}
		req.emit();
		callback = req.createCallback();
		callback.createStage(CONNECTION, start, end, thw, null).emit(); //before end if thrw
		if(nonNull(thw)) { // if connection error
			callback.setEnd(end);
			callback.emit();
		}
	}

	public void handleDisconnection(Instant start, Instant end, Void v, Throwable thw) {
		if(nonNull(callback)) {
			callback.createStage(DISCONNECTION, start, end, thw, null).emit();
			if(callback.assertStillConnected()) { //report if request was closed
				callback.setEnd(end);
				callback.emit(); //avoid emit twice
			}
		}
	}
	
	<T> ExecutionHandler<T> executeStageHandler(MailCommand cmd, Message msg) {
		return (s,e,o,t)-> {
			if(nonNull(callback)) {
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
				callback.assertStillConnected(); //report if request was closed
				callback.createStage(EXECUTE, s, e, t, cmd, mail).emit();
			} //else report
		};
	}
	
	private static String[] toStringArray(Address... address) {
		return isNull(address) || address.length == 0
			? null 
			: Stream.of(address).map(Address::toString).toArray(String[]::new);
	}
}
