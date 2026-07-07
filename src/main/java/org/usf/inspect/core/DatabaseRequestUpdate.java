package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

import java.time.Instant;
import java.util.function.ToIntFunction;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;
import static org.usf.inspect.core.ProtocolErrorHandler.mainCauseException;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class DatabaseRequestUpdate extends AbstractRequestUpdate {

	private boolean failed;
	private int failureCode;
	JdbcErrorHandler  jdbcErrorHandler= new JdbcErrorHandler();
	static final int SUCCESS=-1000;

	@JsonCreator
	public DatabaseRequestUpdate(String id) {
		super(id);
	}
	
	public DatabaseRequestStage createStage(DatabaseAction type, Instant start, Instant end, Throwable thrw, DatabaseCommand cmd, ToIntFunction<Throwable> fn, long[] count) {
		var stg = createStage(type, start, end, thrw,  cmd,fn);
		stg.setCount(count);
		return stg;
	}
		
	public DatabaseRequestStage createStage(DatabaseAction type, Instant start, Instant end, Throwable thrw, DatabaseCommand cmd,  ToIntFunction<Throwable> fn, String... args) {
		if(nonNull(cmd)) {
			setCommand(merge(getCommand(), cmd.getType()));
		}
		Throwable ex = null;
		if(nonNull(thrw)) {
			ex = ExceptionInfo.rootCauseException(thrw);
			try {
			failureCode= fn.applyAsInt(ex);
			} catch (Exception e) {
				failureCode = UNKNOWN_ERROR.getCode();
			}
		} else {
			failureCode = SUCCESS;
		}
		var stg = createStage(type, start, end, cmd, thrw, DatabaseRequestStage::new);
		stg.setArgs(args);
		return stg;
	}

}
