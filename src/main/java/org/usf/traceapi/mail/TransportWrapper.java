package org.usf.traceapi.mail;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.StageTracker.call;
import static org.usf.traceapi.mail.MailAction.CONNECTION;
import static org.usf.traceapi.mail.MailAction.DISCONNECTION;
import static org.usf.traceapi.mail.MailAction.SEND;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.usf.traceapi.core.MailRequest;
import org.usf.traceapi.core.MailRequestStage;
import org.usf.traceapi.core.SafeSupplier.SafeRunnable;
import org.usf.traceapi.core.StageTracker.StageConsumer;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class TransportWrapper  {
	
//	@Delegate
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
			call(()-> trsp.sendMessage(arg0, arg1), appendAction(SEND));
		}
		finally {
			req.setSubject(arg0.getSubject());
			req.setFrom(Stream.of(arg0.getFrom()).map(Address::toString).toArray(String[]::new));
			req.setContentType(arg0.getContentType());
			req.setRecipients(Stream.of(arg0.getAllRecipients()).map(Address::toString).toArray(String[]::new));
			req.setReplyTo(Stream.of(arg0.getReplyTo()).map(Address::toString).toArray(String[]::new));
			req.setSize(arg0.getSize());
		}
	}

	public void close() throws MessagingException {
		try {
			call(trsp::close, appendAction(DISCONNECTION));
		}
		finally {
			req.setEnd(now());
		}
	}
	
	private void connect(String host, Integer port, String user, SafeRunnable<MessagingException> runnable) throws MessagingException {
		try {
			call(runnable, appendAction(CONNECTION));
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
			var fa = new MailRequestStage();
			fa.setName(action.name());
			fa.setStart(s);
			fa.setEnd(e);
			fa.setException(mainCauseException(t));
			req.getActions().add(fa);
		};
	}
	
	private static <T> T requireNonNull(T o, Supplier<T> supp) {
		return isNull(o) ? supp.get() : o;
	}
}
