package org.usf.inspect.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.MailAction.CONNECTION;
import static org.usf.inspect.core.MailAction.DISCONNECTION;
import static org.usf.inspect.core.MailAction.EXECUTE;
import static org.usf.inspect.core.SessionContextManager.createMailRequest;

import java.time.Instant;
import java.util.stream.Stream;

import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.Mail;
import org.usf.inspect.core.MailCommand;
import org.usf.inspect.core.MailRequestCallback;
import org.usf.inspect.core.Monitor;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class MailRequestMonitor implements Monitor {
	
	private final Transport trsp;
	private MailRequestCallback callback;

	public void handleConnection(Instant start, Instant end, Void v, Throwable thw) {
		callback = createMailRequest(start, req->{
			var url = trsp.getURLName();
			if(nonNull(url)) {
				req.setProtocol(url.getProtocol());
				req.setHost(url.getHost());
				req.setPort(url.getPort());
				req.setUser(url.getUsername());
			}
		});
		emit(callback.createStage(CONNECTION, start, end, thw, null)); //before end if thrw
		if(nonNull(thw)) { // if connection error
			callback.setEnd(end);
			emit(callback);
			callback = null;
		}
	}

	public void handleDisconnection(Instant start, Instant end, Void v, Throwable thw) {
		if(assertStillOpened(callback)) { //report if request was closed, avoid emit trace twice
			emit(callback.createStage(DISCONNECTION, start, end, thw, null));
			callback.setEnd(end);
			emit(callback);
			callback = null;
		}
	}
	
	<T> ExecutionHandler<T> executeStageHandler(MailCommand cmd, Message msg) {
		return (s,e,o,t)-> {
			if(assertStillOpened(callback)) { //report if request was closed
				emit(callback.createStage(EXECUTE, s, e, t, cmd, createMailTrace(msg)));
			}
		};
	}
	
	static Mail createMailTrace(Message msg) throws MessagingException {
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
		return mail;
	}
	
	static String[] toStringArray(Address... address) {
		return isNull(address) || address.length == 0
			? null 
			: Stream.of(address).map(Address::toString).toArray(String[]::new);
	}
}
