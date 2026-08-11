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
 * Wraps a mail transport to trace connection and message sending operations.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class TransportWrapper  { //cannot extends jakarta.mail.Transport @see constructor
	
	@Delegate
	private final Transport trsp;
	private MailRequestMonitor monitor;
	
	/**
	 * Connects the wrapped transport using its configured settings.
	 *
	 * @throws MessagingException if the connection fails.
	 */
	public void connect() throws MessagingException {
		this.monitor = new MailRequestMonitor();
		exec(trsp::connect, monitor.handleConnection(trsp));
	}

	/**
	 * Connects the wrapped transport with the specified user credentials.
	 *
	 * @param user the user name to authenticate with.
	 * @param password the password to authenticate with.
	 * @throws MessagingException if the connection fails.
	 */
	public void connect(String user, String password) throws MessagingException {
		this.monitor = new MailRequestMonitor();
		exec(()-> trsp.connect(user, password), monitor.handleConnection(trsp));
	}

	/**
	 * Connects the wrapped transport to the specified host with user credentials.
	 *
	 * @param host the mail server host.
	 * @param user the user name to authenticate with.
	 * @param password the password to authenticate with.
	 * @throws MessagingException if the connection fails.
	 */
	public void connect(String host, String user, String password) throws MessagingException {
		this.monitor = new MailRequestMonitor();
		exec(()-> trsp.connect(host, user, password), monitor.handleConnection(trsp));
	}
	
	/**
	 * Connects the wrapped transport to the specified host and port with user credentials.
	 *
	 * @param arg0 the mail server host.
	 * @param arg1 the mail server port.
	 * @param arg2 the user name to authenticate with.
	 * @param arg3 the password to authenticate with.
	 * @throws MessagingException if the connection fails.
	 */
	public void connect(String arg0, int arg1, String arg2, String arg3) throws MessagingException {
		this.monitor = new MailRequestMonitor();
		exec(()-> trsp.connect(arg0, arg1, arg2, arg3), monitor.handleConnection(trsp));
	}
	
	/**
	 * Sends a message through the wrapped transport while tracing the operation.
	 *
	 * @param arg0 the message to send.
	 * @param arg1 the recipient addresses.
	 * @throws MessagingException if the message cannot be sent.
	 */
	public void sendMessage(Message arg0, Address[] arg1) throws MessagingException {
		exec(()-> trsp.sendMessage(arg0, arg1), monitor.executeStageHandler(SEND, arg0));
	}

	/**
	 * Closes the wrapped transport connection.
	 *
	 * @throws MessagingException if the transport cannot be closed.
	 */
	public void close() throws MessagingException {
		exec(trsp::close, monitor.handleDisconnection());
	}

	/**
	 * Wraps the specified transport with Inspect monitoring.
	 *
	 * @param trsp the transport to wrap.
	 * @return the wrapped transport.
	 */
	public static TransportWrapper wrap(Transport trsp) {
		return wrap(trsp, null);
	}
	
	/**
	 * Wraps the specified transport with Inspect monitoring and an optional bean name.
	 *
	 * @param trsp the transport to wrap.
	 * @param beanName the bean name used for logging, or {@code null} to use a default name.
	 * @return the wrapped transport.
	 */
	public static TransportWrapper wrap(@NonNull Transport trsp, String beanName) {
		if(hub().getConfiguration().isEnabled()){
			logWrappingBean(requireNonNullElse(beanName, "transport"), trsp.getClass());
		}
		return new TransportWrapper(trsp); //cannot implement or extends Transport
	}
}
