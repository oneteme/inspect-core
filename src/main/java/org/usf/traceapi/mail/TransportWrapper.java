package org.usf.traceapi.mail;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.StageTracker.exec;
import static org.usf.traceapi.mail.MailAction.CONNECTION;
import static org.usf.traceapi.mail.MailAction.DISCONNECTION;
import static org.usf.traceapi.mail.MailAction.SEND;

import java.util.LinkedList;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.usf.traceapi.core.Mail;
import org.usf.traceapi.core.MailRequest;
import org.usf.traceapi.core.MailRequestStage;
import org.usf.traceapi.core.SafeCallable.SafeRunnable;
import org.usf.traceapi.core.StageTracker.StageConsumer;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public final class TransportWrapper  {
	
	@Delegate
	private final Transport trsp;
	private final MailRequest req;
	
	public void connect() throws MessagingException {
		connect(null, null, null, trsp::connect);
	}

	public void connect(String user, String password) throws MessagingException {
		connect(null, null, user, ()-> trsp.connect(user, password));
	}

	public void connect(String host, String user, String password) throws MessagingException {
		connect(host, null, host, ()-> trsp.connect(host, user, password));
	}
	
	public void connect(String arg0, int arg1, String arg2, String arg3) throws MessagingException {
		connect(arg0, arg1, arg2, ()-> trsp.connect(arg0, arg1, arg2, arg3));
	}

	public void sendMessage(Message arg0, Address[] arg1) throws MessagingException {
		try {
			exec(()-> trsp.sendMessage(arg0, arg1), appendAction(SEND));
		}
		finally { //safe
			var mail = new Mail();
			mail.setSubject(arg0.getSubject());
			mail.setFrom(toStringArray(arg0.getFrom()));
			mail.setContentType(arg0.getContentType());
			mail.setRecipients(toStringArray(arg0.getAllRecipients()));
			mail.setReplyTo(toStringArray(arg0.getReplyTo()));
			mail.setSize(arg0.getSize());
			req.getMails().add(mail);
		}
	}

	public void close() throws MessagingException {
		try {
			exec(trsp::close, appendAction(DISCONNECTION));
		}
		finally {
			req.setEnd(now());
		}
	}
	
	private void connect(String host, Integer port, String user, SafeRunnable<MessagingException> runnable) throws MessagingException {
		try {
			exec(runnable, appendAction(CONNECTION));
		}
		finally {
			var url = trsp.getURLName();
			if(isNull(url)) {
				req.setHost(host);
				req.setPort(port);
				req.setUser(user);
			}
			else {
				req.setProtocol(url.getProtocol());
				req.setHost(requireNonNull(host, url::getHost));
				req.setPort(requireNonNull(port, url::getPort));
				req.setUser(requireNonNull(user, url::getUsername));
			}
		}
	}
	
	<T> StageConsumer<T> appendAction(MailAction action) {
		return (s,e,o,t)-> {
			var rs = new MailRequestStage();
			rs.setName(action.name());
			rs.setStart(s);
			rs.setEnd(e);
			rs.setException(mainCauseException(t));
			req.getActions().add(rs);
		};
	}
	
	public static TransportWrapper wrap(Transport trsp) {
		var req = new MailRequest();
		req.setActions(new LinkedList<>());
		req.setMails(new LinkedList<>());
		return new TransportWrapper(trsp, req);
	}
	
	private static <T> T requireNonNull(T o, Supplier<T> supp) {
		return isNull(o) ? supp.get() : o;
	}
	
	private static String[] toStringArray(Address... address) {
		return isNull(address) 
			? null 
			: Stream.of(address).map(Address::toString).toArray(String[]::new);
	}
}
