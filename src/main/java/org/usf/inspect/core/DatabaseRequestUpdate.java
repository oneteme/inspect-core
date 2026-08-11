package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * Mutable update data for a database request.
 */
@Getter
@Setter
public final class DatabaseRequestUpdate extends AbstractRequestUpdate {

	private boolean failed;
	
	/**
	 * Creates a database request update.
	 *
	 * @param id the request identifier
	 */
	@JsonCreator
	public DatabaseRequestUpdate(String id) {
		super(id);
	}
	
	/**
	 * Creates a database request stage with row counts.
	 *
	 * @param type the database action
	 * @param start the stage start time
	 * @param end the stage end time
	 * @param thrw the failure cause, if any
	 * @param cmd the database command
	 * @param count the affected row counts
	 * @return the created stage
	 */
	public DatabaseRequestStage createStage(DatabaseAction type, Instant start, Instant end, Throwable thrw, DatabaseCommand cmd, long[] count) {
		var stg = createStage(type, start, end, thrw, cmd);
		stg.setCount(count);
		return stg;
	}
		
	/**
	 * Creates a database request stage with command arguments.
	 *
	 * @param type the database action
	 * @param start the stage start time
	 * @param end the stage end time
	 * @param thrw the failure cause, if any
	 * @param cmd the database command
	 * @param args the command arguments
	 * @return the created stage
	 */
	public DatabaseRequestStage createStage(DatabaseAction type, Instant start, Instant end, Throwable thrw, DatabaseCommand cmd, String... args) {
		if(nonNull(cmd)) {
			setCommand(merge(getCommand(), cmd.getType()));
		}
		if(nonNull(thrw)) {
			failed = true; 
		}
		var stg = createStage(type, start, end, cmd, thrw, DatabaseRequestStage::new);
		stg.setArgs(args);
		return stg;
	}
}
