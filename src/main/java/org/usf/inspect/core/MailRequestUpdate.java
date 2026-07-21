package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;
import static org.usf.inspect.core.ErrorCode.SUCCESS;


import java.time.Instant;
import java.util.function.ToIntFunction;

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
	//private int status;


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
				setStatus(fn.applyAsInt(ex));
			} catch (Exception e) {
				setStatus(UNKNOWN_ERROR.getCode());
			}
		}
		else {
			setStatus(SUCCESS.getCode());
		}
		return createStage(action, start, end, cmd, ex, MailRequestStage::new);
	}
}
