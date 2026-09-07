package org.usf.inspect.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.MailAction.CONNECTION;
import static org.usf.inspect.core.MailAction.DISCONNECTION;
import static org.usf.inspect.core.MailAction.EXECUTE;
import static org.usf.inspect.core.SessionContextManager.createMailSignal;

import java.time.Instant;
import java.util.stream.Stream;

import org.usf.inspect.core.ConnectionLifecycleTracer;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Mail;
import org.usf.inspect.core.MailAction;
import org.usf.inspect.core.MailCommand;
import org.usf.inspect.core.MailRequestSignal;
import org.usf.inspect.core.MailRequestStage;
import org.usf.inspect.core.MailRequestUpdate;
import org.usf.inspect.core.TraceSignal;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor
final class MailConnectionLifecycleTracer extends ConnectionLifecycleTracer {

	@Override
	protected MailRequestSignal signal(Instant start) {
		return createMailSignal(start);
	}

	@Override
	protected MailRequestUpdate update(TraceSignal signal) { 
		return new MailRequestUpdate(signal.getId());
	}
	
	@Override
	public short resolveStatus(Throwable t) {
	    return switch (t) {
	        case jakarta.mail.AuthenticationFailedException e -> CLIENT_UNAUTHORIZED;
	        case jakarta.mail.internet.ParseException e -> CLIENT_ERROR;
	        case jakarta.mail.MessagingException e -> SERVER_ERROR;
			default -> super.resolveStatus(t);
	    };
	}
	
	public ExecutionListener<Void> connectionListener(Transport trsp) {
		return connectionListener(stageBuilder(CONNECTION, null, null), (trc,cnx)->{
			var sgn = (MailRequestSignal) trc;
			if(nonNull(trsp)) {
				var url = trsp.getURLName();
				if(nonNull(url)) {
					sgn.setProtocol(url.getProtocol());
					sgn.setHost(url.getHost());
					sgn.setPort(url.getPort());
					sgn.setUser(url.getUsername());
				}
			}
		});
	}
	
	public ExecutionListener<Void> disconnectionListener() {
		return disconnectionListener(stageBuilder(DISCONNECTION, null, null));
	}

	public <T> ExecutionListener<T> executeStageListener(MailCommand cmd, Message msg) {
		return stageListener(EXECUTE, cmd, msg);
	}
	
	public <T> ExecutionListener<T> stageListener(MailAction action, MailCommand cmd, Message msg) {
		if(nonNull(cmd) && nonNull(getUpdate())) {
			var upd = (MailRequestUpdate) getUpdate();
			upd.setCommand(merge(upd.getCommand(), cmd.getType()));
		}
		return stageListener(stageBuilder(action, cmd, msg));
	}
	
	<R> StageBuilder<R> stageBuilder(MailAction action, MailCommand cmd, Message msg) {
		return (s,e,o,t)-> {
			var stg = new MailRequestStage(getUpdate().getId(), getStageCounter().incrementAndGet());
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			if(nonNull(cmd)) {
				stg.setCommand(cmd.name());
			}
			stg.setPayload(null);
			stg.setMail(mailTrace(msg));
			return stg;
		};
	}
	
	static Mail mailTrace(Message msg) throws MessagingException {
		if(nonNull(msg)) {
			var mail = new Mail();
			mail.setSubject(msg.getSubject());
			mail.setFrom(toStringArray(msg.getFrom()));
			mail.setRecipients(toStringArray(msg.getAllRecipients()));
			mail.setReplyTo(toStringArray(msg.getReplyTo()));
			mail.setContentType(msg.getContentType());
			mail.setSize(msg.getSize());
			return mail;
		}
		return null;
	}
	
	static String[] toStringArray(Address... address) {
		return isNull(address)
			? null 
			: Stream.of(address).map(Address::toString).toArray(String[]::new);
	}
}
