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
public final class MainSessionUpdate extends AbstractSessionUpdate {

	private final boolean startup;

	@JsonCreator
	public MainSessionUpdate(String id) {
		this(id, false);
	}
	
	//protected for startup use case
	MainSessionUpdate(String id, boolean startup) {
		super(id);
		this.startup = startup;
	}
	
	@Override
	public boolean isStartup() {
		return startup;
	}
}
