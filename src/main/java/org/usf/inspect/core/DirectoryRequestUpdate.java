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
public final class DirectoryRequestUpdate extends AbstractRequestUpdate {

	@Deprecated(forRemoval = false, since = "1.2")
	private boolean failed;

	@JsonCreator
	public DirectoryRequestUpdate(String id) {
		super(id);
	}

	public DirectoryRequestStage createStage(){
		return new DirectoryRequestStage(getId(), getStageCounter().getAndIncrement());
	}
}
