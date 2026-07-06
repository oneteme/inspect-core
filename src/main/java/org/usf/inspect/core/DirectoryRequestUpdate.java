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
public final class DirectoryRequestUpdate extends AbstractRequestUpdate {

	 private boolean failed;
	 private int failureCode;
	LdapErrorHandler ldapErrorHandler= new LdapErrorHandler();

	@JsonCreator
	public DirectoryRequestUpdate(String id) {
		super(id);
	}

	public DirectoryRequestStage createStage(DirAction type, Instant start, Instant end, Throwable thrw, DirCommand cmd, String... args) {
		if(nonNull(cmd)) {
			setCommand(merge(getCommand(), cmd.getType()));
		}
		if(nonNull(thrw)) {
			failed = true;
		    try{
			failureCode = ldapErrorHandler.checkException(mainCauseException(thrw));
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
