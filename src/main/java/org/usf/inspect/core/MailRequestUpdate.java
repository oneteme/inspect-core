package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;
import static org.usf.inspect.core.ErrorCode.SUCCESS;

import static org.usf.inspect.core.ProtocolErrorHandler.mainCauseException;

import java.time.Instant;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.mail.MailErrorHandler;


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
	private  ProtocolErrorHandler mailErrorHandler = new MailErrorHandler();

	@JsonCreator
	public MailRequestUpdate(String id) {
		super(id);
	}

	public MailRequestStage createStage(MailAction action, Instant start, Instant end, Throwable thrw, MailCommand cmd, Mail mail, ToIntFunction<Throwable> fn) {
		var stg = createStage(action, start, end, thrw, cmd, fn);
		stg.setMail(mail);
		return stg;
	}
	
	public MailRequestStage createStage(MailAction action, Instant start, Instant end, Throwable thrw, MailCommand cmd, ToIntFunction<Throwable> fn) {
		if(nonNull(cmd)) {
			setCommand(merge(getCommand(), cmd.getType()));
		}
		Throwable ex = null;
		if(nonNull(thrw)) {
			ex = ExceptionInfo.rootCauseException(thrw);
			try{
				failureCode = fn.applyAsInt(ex);
			} catch (Exception e) {
				failureCode = UNKNOWN_ERROR.getCode();
			}
		}
		else {
			failureCode = SUCCESS.getCode();
		}
		return createStage(action, start, end, cmd, ex, MailRequestStage::new);
	}
}
