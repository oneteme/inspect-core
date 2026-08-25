package org.usf.inspect.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.Helper.rootCauseException;
import static org.usf.inspect.core.MailAction.CONNECTION;
import static org.usf.inspect.core.MailAction.DISCONNECTION;
import static org.usf.inspect.core.MailAction.EXECUTE;
import static org.usf.inspect.core.RequestCommonStatus.CLIENT_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.CLIENT_UNAUTHORIZED;
import static org.usf.inspect.core.RequestCommonStatus.SERVER_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.SUCCESS;
import static org.usf.inspect.core.RequestCommonStatus.statusFor;

import java.util.stream.Stream;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Mail;
import org.usf.inspect.core.MailAction;
import org.usf.inspect.core.MailCommand;
import org.usf.inspect.core.MailRequestSignal;
import org.usf.inspect.core.MailRequestUpdate;
import org.usf.inspect.core.Monitor.StatefulMonitor;
import org.usf.inspect.core.SessionContextManager;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;

/**
 * 
 * @author u$f
 *
 */
final class MailRequestMonitor extends StatefulMonitor<MailRequestSignal, MailRequestUpdate> {

	ExecutionListener<Object> handleConnection(Transport trsp) {
		return traceBegin(SessionContextManager::createMailRequest, (req,v)->{
			var url = trsp.getURLName();
			if(nonNull(url)) {
				req.setProtocol(url.getProtocol());
				req.setHost(url.getHost());
				req.setPort(url.getPort());
				req.setUser(url.getUsername());
			}
		}, stageHandler(CONNECTION, null, null)); //before end if thrw
	}
	
	protected MailRequestUpdate createCallback(MailRequestSignal session) { 
		return session.createCallback();
	}

	ExecutionListener<Object> handleDisconnection() {
		return traceEnd(stageHandler(DISCONNECTION, null, null));
	}
	
	<T> ExecutionListener<T> executeStageHandler(MailCommand cmd, Message msg) {
		return stageHandler(EXECUTE, cmd, msg);
	}
	
	<T> ExecutionListener<T> stageHandler(MailAction action, MailCommand cmd, Message msg) {
		return traceStep((s,e,o,t)-> {
			var upd = getCallback();
			var stg = upd.createStage();
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			if(nonNull(cmd)) {
				stg.setCommand(cmd.name());
				upd.setCommand(merge(upd.getCommand(), cmd.getType()));
			}
			if(nonNull(t)) {
				var root = rootCauseException(t);
				stg.setException(fromException(root, 0, 0)); //no stack trace
				if(upd.getStatus() < 0 ||  upd.getStatus() == SUCCESS) {
					upd.setStatus(resolveStatus(root));
				}
			}
			else {
				upd.setStatus(SUCCESS);
			}
			stg.setMail(createMailTrace(msg));
			return stg;
		});
	}
	
	static Mail createMailTrace(Message msg) throws MessagingException {
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

	static int resolveStatus(Throwable t) {
	    if (isNull(t)) {
	        return SUCCESS;
	    }
	    return switch (t) {
	        case jakarta.mail.AuthenticationFailedException e -> CLIENT_UNAUTHORIZED;
	        case jakarta.mail.internet.ParseException e -> CLIENT_ERROR;
	        case jakarta.mail.MessagingException e -> SERVER_ERROR;
			default -> statusFor(t);
	    };
	}
	
	static String[] toStringArray(Address... address) {
		return isNull(address) || address.length == 0
			? null 
			: Stream.of(address).map(Address::toString).toArray(String[]::new);
	}
}
