package org.usf.inspect.mail;

import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.MailCommand.SEND;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class TransportWrapper  { //cannot extends jakarta.mail.Transport @see constructor
	
	@Delegate
	private final Transport trsp;
	private final MailConnectionLifecycleTracer listener;

	public TransportWrapper(Transport trsp) {
		this.trsp = trsp;
		this.listener = new MailConnectionLifecycleTracer();
	}
	
	public void connect() throws MessagingException {
		exec(trsp::connect, listener.connectionListener(trsp));
	}

	public void connect(String user, String password) throws MessagingException {
		exec(()-> trsp.connect(user, password), listener.connectionListener(trsp));
	}

	public void connect(String host, String user, String password) throws MessagingException {
		exec(()-> trsp.connect(host, user, password), listener.connectionListener(trsp));
	}
	
	public void connect(String arg0, int arg1, String arg2, String arg3) throws MessagingException {
		exec(()-> trsp.connect(arg0, arg1, arg2, arg3), listener.connectionListener(trsp));
	}
	
	public void sendMessage(Message arg0, Address[] arg1) throws MessagingException {
		exec(()-> trsp.sendMessage(arg0, arg1), listener.executeStageListener(SEND, arg0));
	}

	public void close() throws MessagingException {
		exec(trsp::close, listener.disconnectionListener());
	}

	public static TransportWrapper wrap(Transport trsp) {
		return wrap(trsp, null);
	}
	
	public static TransportWrapper wrap(@NonNull Transport trsp, String beanName) {
		if(hub().getConfiguration().isEnabled()){
			logWrappingBean(requireNonNullElse(beanName, "transport"), trsp.getClass());
		}
		return new TransportWrapper(trsp); //cannot implement or extends Transport
	}
}
