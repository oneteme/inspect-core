package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

import java.time.Instant;

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
			try {
			failureCode= jdbcErrorHandler.checkException(mainCauseException(thrw));
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
