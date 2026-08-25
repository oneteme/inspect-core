package org.usf.inspect.core;

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
	public DatabaseRequestUpdate(String id) {
		super(id);
	}
	
	public DatabaseRequestStage createStage(){
		return new DatabaseRequestStage(getId(), getStageCounter().getAndIncrement());
	}
}
