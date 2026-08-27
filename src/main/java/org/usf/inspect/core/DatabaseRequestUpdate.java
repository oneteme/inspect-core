package org.usf.inspect.core;

import java.util.UUID;

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

	@Deprecated(forRemoval = false, since = "1.2")
	private boolean failed;

	@JsonCreator
	public DatabaseRequestUpdate(UUID id) {
		super(id);
	}

	@Deprecated(forRemoval = true, since = "1.2")
	public DatabaseRequestStage createStage(){
		return new DatabaseRequestStage(getId(), getStageCounter().getAndIncrement());
	}
}
