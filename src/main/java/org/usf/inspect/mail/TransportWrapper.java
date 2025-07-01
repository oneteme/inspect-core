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
import java.util.function.Consumer;
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
public final class TransportWrapper  { //cannot extends jakarta.mail.Transport @see constructor
	
	@Delegate
	private final Transport trsp;
	private MailRequest req;
	
	public void connect() throws MessagingException {
		exec(trsp::connect, smtpRequestListener(null, null, null));
	}

	public void connect(String user, String password) throws MessagingException {
		exec(()-> trsp.connect(user, password), smtpRequestListener(null, null, user));
	}

	public void connect(String host, String user, String password) throws MessagingException {
		exec(()-> trsp.connect(host, user, password), smtpRequestListener(host, null, user));
	}
	
	public void connect(String arg0, int arg1, String arg2, String arg3) throws MessagingException {
		exec(()-> trsp.connect(arg0, arg1, arg2, arg3), smtpRequestListener(arg0, arg1, arg2));
	}
	
	public void sendMessage(Message arg0, Address[] arg1) throws MessagingException {
		exec(()-> trsp.sendMessage(arg0, arg1), (s,e,o,t)->{
			var mail = new Mail(); // broke Mail dependency !?
			mail.setSubject(arg0.getSubject());
			mail.setFrom(toStringArray(arg0.getFrom()));
			mail.setRecipients(toStringArray(arg0.getAllRecipients()));
			mail.setReplyTo(toStringArray(arg0.getReplyTo()));
			mail.setContentType(arg0.getContentType());
			mail.setSize(arg0.getSize());
			var stg = smtpStage(SEND, s, e, t);
			submit(ses->{
				req.append(stg);
				req.getMails().add(mail);
			});
		});
	}

	public void close() throws MessagingException {
		exec(trsp::close, (s,e,o,t)-> {
			var stg = smtpStage(DISCONNECTION, s, e, t);
			submit(ses-> {
				req.append(stg);
				req.setEnd(e);
			});
		});
	}
	
	ExecutionMonitorListener<Void> smtpRequestListener(String host, Integer port, String user) {
		req = new MailRequest(); 
		return (s,e,o,t)->{
			req.setThreadName(threadName());
			req.setStart(s);
			if(nonNull(t)) { // if connection error
				req.setEnd(e);
			}
			else {
				req.setMails(new ArrayList<>(1));
			}
			var url = trsp.getURLName(); //broke trsp dependency
			if(nonNull(url)) {
				req.setProtocol(url.getProtocol());
				req.setHost(url.getHost());
				req.setPort(url.getPort());
				req.setUser(url.getUsername());
			}
			acceptIfNonNull(host, req::setHost);
			acceptIfNonNull(port, req::setPort);
			acceptIfNonNull(user, req::setUser);
			req.setActions(new ArrayList<>(nonNull(t) ? 1 : 3)); //cnx, send, dec
			req.append(smtpStage(CONNECTION, s, e, t));
			submit(req);
		};
	}

	static MailRequestStage smtpStage(MailAction action, Instant start, Instant end, Throwable t) {
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
	
	static <T> void acceptIfNonNull(T o, Consumer<T> cons) {
		if(nonNull(o)) {
			cons.accept(o);
		}
	}
}
