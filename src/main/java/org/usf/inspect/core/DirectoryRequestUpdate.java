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
@Getter
@Setter
public final class DirectoryRequestUpdate extends AbstractRequestUpdate {

	 private boolean failed;
	 private int failureCode;

	@JsonCreator
	public DirectoryRequestUpdate(String id) {
		super(id);
	}

	public DirectoryRequestStage createStage(DirAction type, Instant start, Instant end, Throwable thrw, DirCommand cmd, ToIntFunction<Throwable> fn, String... args) {
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
		var stg = createStage(type, start, end, cmd, thrw, DirectoryRequestStage::new);
		stg.setArgs(args);
		return stg;
	}	
}
