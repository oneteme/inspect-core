package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * Mutable update data for a directory request.
 */
@Getter
@Setter
public final class DirectoryRequestUpdate extends AbstractRequestUpdate {

	 private boolean failed;

	/**
	 * Creates a directory request update.
	 *
	 * @param id the request identifier
	 */
	@JsonCreator
	public DirectoryRequestUpdate(String id) {
		super(id);
	}

	/**
	 * Creates a directory request stage.
	 *
	 * @param type the directory action
	 * @param start the stage start time
	 * @param end the stage end time
	 * @param thrw the failure cause, if any
	 * @param cmd the directory command
	 * @param args the command arguments
	 * @return the created stage
	 */
	public DirectoryRequestStage createStage(DirAction type, Instant start, Instant end, Throwable thrw, DirCommand cmd, String... args) {
		if(nonNull(cmd)) {
			setCommand(merge(getCommand(), cmd.getType()));
		}
		if(nonNull(thrw)) {
			failed = true; 
		}
		var stg = createStage(type, start, end, cmd, thrw, DirectoryRequestStage::new);
		stg.setArgs(args);
		return stg;
	}	
}
