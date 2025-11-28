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
public class DirectoryRequestCallback extends AbstractRequestCallback {

	private boolean failed;

	@JsonCreator
	public DirectoryRequestCallback(String id) {
		super(id);
	}

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
