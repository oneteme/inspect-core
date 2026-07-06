package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;
import static org.usf.inspect.core.ErrorCode.SUCCESS;

import static org.usf.inspect.core.ProtocolErrorHandler.mainCauseException;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;


/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
public final class MailRequestUpdate extends AbstractRequestUpdate {

	private boolean failed;
	private int failureCode;
	private  MailErrorHandler mailErrorHandler = new MailErrorHandler();

	@JsonCreator
	public MailRequestUpdate(String id) {
		super(id);
	}

	public MailRequestStage createStage(MailAction action, Instant start, Instant end, Throwable thrw, MailCommand cmd, Mail mail) {
		var stg = createStage(action, start, end, thrw, cmd);
		stg.setMail(mail);
		return stg;
	}
	
	public MailRequestStage createStage(MailAction action, Instant start, Instant end, Throwable thrw, MailCommand cmd) {
		if(nonNull(cmd)) {
			setCommand(merge(getCommand(), cmd.getType()));
		}
		if(nonNull(thrw)) {
			try{
			failed = true;
			failureCode = mailErrorHandler.checkException(mainCauseException(thrw));
			} catch (Exception e) {
				failureCode = UNKNOWN_ERROR.getCode();
			}
		}
		else {
			failureCode = SUCCESS.getCode();
		}
		return createStage(action, start, end, cmd, thrw, MailRequestStage::new);
	}
}
