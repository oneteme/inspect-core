package org.usf.inspect.core;

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
public final class MainSessionUpdate extends AbstractSessionUpdate {

	private Instant start; //updated sometime after initialization

	@JsonCreator
	public MainSessionUpdate(String id) {
		this(id, false);
	}
	
	MainSessionUpdate(String id, boolean startup) {
		super(id, startup);
	}
}
