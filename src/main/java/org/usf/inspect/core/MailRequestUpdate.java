package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * Collects update information for a mail request as its stages complete.
 *
 * @author u$f
 *
 */
@Setter
@Getter
public final class MailRequestUpdate extends AbstractRequestUpdate {

	private boolean failed;

	/**
	 * Creates a mail request update for the supplied request identifier.
	 *
	 * @param id the request identifier
	 */
	@JsonCreator
	public MailRequestUpdate(String id) {
		super(id);
	}

	/**
	 * Creates a request stage and attaches the processed mail message to it.
	 *
	 * @param action the action executed by the stage
	 * @param start the stage start time
	 * @param end the stage end time
	 * @param thrw the exception raised during the stage, if any
	 * @param cmd the mail command associated with the stage
	 * @param mail the mail message associated with the stage
	 * @return the created stage populated with the mail message
	 */
	public MailRequestStage createStage(MailAction action, Instant start, Instant end, Throwable thrw, MailCommand cmd, Mail mail) {
		var stg = createStage(action, start, end, thrw, cmd);
		stg.setMail(mail);
		return stg;
	}
	
	/**
	 * Creates a request stage and updates the aggregate request status from the stage outcome.
	 *
	 * @param action the action executed by the stage
	 * @param start the stage start time
	 * @param end the stage end time
	 * @param thrw the exception raised during the stage, if any
	 * @param cmd the mail command associated with the stage
	 * @return the created request stage
	 */
	public MailRequestStage createStage(MailAction action, Instant start, Instant end, Throwable thrw, MailCommand cmd) {
		if(nonNull(cmd)) {
			setCommand(merge(getCommand(), cmd.getType()));
		}
		if(nonNull(thrw)) {
			failed = true; 
		}
		return createStage(action, start, end, cmd, thrw, MailRequestStage::new);
	}
}
