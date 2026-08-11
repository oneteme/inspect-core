package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * Mutable update data for an FTP request.
 */
@Getter
@Setter
public final class FtpRequestUpdate extends AbstractRequestUpdate {

	private boolean failed;

	/**
	 * Creates an FTP request update.
	 *
	 * @param id the request identifier
	 */
	@JsonCreator
	public FtpRequestUpdate(String id) {
		super(id);
	}

	/**
	 * Creates an FTP request stage.
	 *
	 * @param type the FTP action
	 * @param start the stage start time
	 * @param end the stage end time
	 * @param thrw the failure cause, if any
	 * @param cmd the FTP command
	 * @param args the command arguments
	 * @return the created stage
	 */
	public FtpRequestStage createStage(FtpAction type, Instant start, Instant end, Throwable thrw, FtpCommand cmd, String... args) {
		if(nonNull(cmd)) {
			setCommand(merge(getCommand(), cmd.getType()));
		}
		if(nonNull(thrw)) {
			failed = true; 
		}
		var stg = createStage(type, start, end, cmd, thrw, FtpRequestStage::new);
		stg.setArgs(args);
		return stg;
	}
}
