package org.usf.inspect.mail;

import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.mail.MailAction.SEND;

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
	private MailRequestMonitor monitor;
	
	public void connect() throws MessagingException {
		this.monitor = new MailRequestMonitor(trsp);
		exec(trsp::connect, monitor::handleConnection);
	}

	public void connect(String user, String password) throws MessagingException {
		this.monitor = new MailRequestMonitor(trsp);
		exec(()-> trsp.connect(user, password), monitor::handleConnection);
	}

	public void connect(String host, String user, String password) throws MessagingException {
		this.monitor = new MailRequestMonitor(trsp);
		exec(()-> trsp.connect(host, user, password), monitor::handleConnection);
	}
	
	public void connect(String arg0, int arg1, String arg2, String arg3) throws MessagingException {
		this.monitor = new MailRequestMonitor(trsp);
		exec(()-> trsp.connect(arg0, arg1, arg2, arg3), monitor::handleConnection);
	}
	
	public void sendMessage(Message arg0, Address[] arg1) throws MessagingException {
		exec(()-> trsp.sendMessage(arg0, arg1), monitor.stageHandler(SEND, arg0));
	}

	public void close() throws MessagingException {
		exec(trsp::close, monitor::handleDisconnection);
	}
	
	public static TransportWrapper wrap(Transport trsp) {
		if(context().getConfiguration().isEnabled()){
			logWrappingBean("transport", trsp.getClass());
		}
		return new TransportWrapper(trsp); //cannot implement or extends Transport
	}
}
