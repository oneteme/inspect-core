package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.ProtocolErrorHandler.mainCauseException;

import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;
import static org.usf.inspect.core.ErrorCode.SUCCESS;

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
public final class FtpRequestUpdate extends AbstractRequestUpdate {

	private boolean failed;
	FtpErrorHandler ftpErrorHandler= new FtpErrorHandler();
	private int failureCode;

	@JsonCreator
	public FtpRequestUpdate(String id) {
		super(id);
	}

	public FtpRequestStage createStage(FtpAction type, Instant start, Instant end, Throwable thrw, FtpCommand cmd, String... args) {
		if(nonNull(cmd)) {
			setCommand(merge(getCommand(), cmd.getType()));
		}
		if(nonNull(thrw)) {
			failed = true;
			try{
			failureCode= ftpErrorHandler.checkException(mainCauseException(thrw));
		} catch (Exception e) {
			failureCode = UNKNOWN_ERROR.getCode();
		  }
		} else {
			failureCode = SUCCESS.getCode();
		}
		var stg = createStage(type, start, end, cmd, thrw, FtpRequestStage::new);
		stg.setArgs(args);
		return stg;
	}
}
