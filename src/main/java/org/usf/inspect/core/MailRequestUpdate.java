package org.usf.inspect.core;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;


/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
public final class MailRequestUpdate extends AbstractRequestUpdate {

	@Deprecated(forRemoval = false, since = "1.2")
	private boolean failed;

	@JsonCreator
	public MailRequestUpdate(String id) {
		super(id);
	}

	public MailRequestStage createStage(){
		return new MailRequestStage(getId(), getStageCounter().getAndIncrement());
	}
}
