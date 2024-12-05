package org.usf.inspect.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.requestAppender;
import static org.usf.inspect.core.StageTracker.exec;
import static org.usf.inspect.mail.MailAction.CONNECTION;
import static org.usf.inspect.mail.MailAction.DISCONNECTION;
import static org.usf.inspect.mail.MailAction.SEND;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.usf.inspect.core.Mail;
import org.usf.inspect.core.MailRequest;
import org.usf.inspect.core.MailRequestStage;
import org.usf.inspect.core.StageTracker.StageCreator;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
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
		exec(trsp::connect, toMailRequest(null, null, null), requestAppender());
	}

	public void connect(String user, String password) throws MessagingException {
		exec(()->trsp.connect(user, password), toMailRequest(null, null, user), requestAppender());
	}

	public void connect(String host, String user, String password) throws MessagingException {
		exec(()-> trsp.connect(host, user, password), toMailRequest(host, null, user), requestAppender());
	}
	
	public void connect(String arg0, int arg1, String arg2, String arg3) throws MessagingException {
		exec(()-> trsp.connect(arg0, arg1, arg2, arg3), toMailRequest(arg0, arg1, arg2), requestAppender());
	}

	public void close() throws MessagingException {
		exec(trsp::close, mailActionCreator(DISCONNECTION), stg-> {
			req.append(stg);
			req.setEnd(stg.getEnd());
		});
	}
	
	public void sendMessage(Message arg0, Address[] arg1) throws MessagingException {
		exec(()-> trsp.sendMessage(arg0, arg1), mailActionCreator(SEND), stg->{
			req.append(stg); //first
			var mail = new Mail();
			mail.setSubject(arg0.getSubject());
			mail.setFrom(toStringArray(arg0.getFrom()));
			mail.setRecipients(toStringArray(arg0.getAllRecipients()));
			mail.setReplyTo(toStringArray(arg0.getReplyTo()));
			mail.setContentType(arg0.getContentType());
			mail.setSize(arg0.getSize());
			req.getMails().add(mail);
		});
	}
	
	StageCreator<Void, MailRequest> toMailRequest(String host, Integer port, String user) {
		return (s,e,v,t)-> {
			req = new MailRequest();
			req.setStart(s);
			if(nonNull(t)) { // fail: do not setException, already set in action
				req.setEnd(e);
			}
			req.setThreadName(threadName());
			var url = ofNullable(trsp.getURLName());
			req.setHost(url.map(URLName::getHost).orElse(host));
			req.setPort(url.map(URLName::getPort).orElse(port));
			req.setUser(url.map(URLName::getUsername).orElse(user));
			req.setActions(new ArrayList<>());
			req.setMails(new ArrayList<>());
			req.append(mailActionCreator(CONNECTION).create(s, e, v, t));
			return req;
		};
	}
	
	StageCreator<Void, MailRequestStage> mailActionCreator(MailAction action) {
		return (s,e,o,t)-> {
			var stg = new MailRequestStage();
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			if(nonNull(t)) {
				stg.setException(mainCauseException(t));
			}
			return stg;
		};
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
