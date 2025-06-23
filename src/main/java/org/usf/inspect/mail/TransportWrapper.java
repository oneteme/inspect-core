package org.usf.inspect.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.submit;
import static org.usf.inspect.mail.MailAction.CONNECTION;
import static org.usf.inspect.mail.MailAction.DISCONNECTION;
import static org.usf.inspect.mail.MailAction.SEND;

import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.Mail;
import org.usf.inspect.core.MailRequest;
import org.usf.inspect.core.MailRequestStage;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class TransportWrapper { //cannot extends jakarta.mail.Transport
	
	@Delegate
	private final Transport trsp;
	private MailRequest req;
	
	public void connect() throws MessagingException {
		exec(trsp::connect, toMailRequest(null, null, null));
	}

	public void connect(String user, String password) throws MessagingException {
		exec(()-> trsp.connect(user, password), toMailRequest(null, null, user));
	}

	public void connect(String host, String user, String password) throws MessagingException {
		exec(()-> trsp.connect(host, user, password), toMailRequest(host, null, user));
	}
	
	public void connect(String arg0, int arg1, String arg2, String arg3) throws MessagingException {
		exec(()-> trsp.connect(arg0, arg1, arg2, arg3), toMailRequest(arg0, arg1, arg2));
	}

	public void close() throws MessagingException {
		exec(trsp::close, (s,e,o,t)-> submit(ses-> {
			req.append(newStage(DISCONNECTION, s, e, t));
			req.setEnd(e);
		}));
	}
	
	public void sendMessage(Message arg0, Address[] arg1) throws MessagingException {
		exec(()-> trsp.sendMessage(arg0, arg1), (s,e,o,t)-> submit(ses->{
			req.append(newStage(SEND, s, e, t));
			var mail = new Mail(); // broke Mail dependency !?
			mail.setSubject(arg0.getSubject());
			mail.setFrom(toStringArray(arg0.getFrom()));
			mail.setRecipients(toStringArray(arg0.getAllRecipients()));
			mail.setReplyTo(toStringArray(arg0.getReplyTo()));
			mail.setContentType(arg0.getContentType());
			mail.setSize(arg0.getSize());
			req.getMails().add(mail);
		}));
	}
	
	ExecutionMonitorListener<Void> toMailRequest(String host, Integer port, String user) {
		req = new MailRequest();
		return (s,e,o,t)->{
			req.setThreadName(threadName()); 
			var url = trsp.getURLName(); //broke trsp dependency
			submit(ses-> {
				req.setStart(s);
				if(nonNull(t)) { // if connection error
					req.setEnd(e);
				}
				else {
					req.setMails(new ArrayList<>(1));
				}
				if(nonNull(url)) {
					req.setProtocol(url.getProtocol());
					req.setHost(url.getHost());
					req.setPort(url.getPort());
					req.setUser(url.getUsername());
				}
				else {
					req.setHost(host);
					req.setPort(port);
					req.setUser(user);
				}
				req.setActions(new ArrayList<>(nonNull(t) ? 1 : 3)); //cnx, send, dec
				req.append(newStage(CONNECTION, s, e, t));
				ses.append(req);
			});
		};
	}

	static MailRequestStage newStage(MailAction action, Instant start, Instant end, Throwable t) {
		var stg = new MailRequestStage();
		stg.setName(action.name());
		stg.setStart(start);
		stg.setEnd(end);
		if(nonNull(t)) {
			stg.setException(mainCauseException(t));
		}
		return stg;
	}
	
	private static String[] toStringArray(Address... address) {
		return isNull(address) || address.length == 0
			? null 
			: Stream.of(address).map(Address::toString).toArray(String[]::new);
	}
	
	public static TransportWrapper wrap(Transport trsp) {
		return new TransportWrapper(trsp);
	}
}
