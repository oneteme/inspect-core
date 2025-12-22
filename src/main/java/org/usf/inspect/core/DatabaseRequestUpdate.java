package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class DatabaseRequestUpdate extends AbstractRequestUpdate {

	private boolean failed;
	
	@JsonCreator
	public DatabaseRequestUpdate(String id) {
		super(id);
	}
	
	public DatabaseRequestStage createStage(DatabaseAction type, Instant start, Instant end, Throwable thrw, DatabaseCommand cmd, long[] count) {
		var stg = createStage(type, start, end, thrw, cmd);
		stg.setCount(count);
		return stg;
	}
		
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
