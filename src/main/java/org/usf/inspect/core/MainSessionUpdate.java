package org.usf.inspect.core;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * Collects completion information for a main session.
 *
 * @author u$f
 *
 */
@Getter
@Setter
public final class MainSessionUpdate extends AbstractSessionUpdate {

	private final boolean startup;

	/**
	 * Creates a main session update for the supplied session identifier.
	 *
	 * @param id the session identifier
	 */
	@JsonCreator
	public MainSessionUpdate(String id) {
		this(id, false);
	}

	/**
	 * Also creates main session update, but method protected when the param startup is true (enum from MainSessionType.java)
	 * @param id
	 * @param startup
	 */
	MainSessionUpdate(String id, boolean startup) {
		super(id);
		this.startup = startup;
	}
	
	/**
	 * Indicates whether the session represents application startup processing.
	 *
	 * @return {@code true} if the session is a startup session; {@code false} otherwise
	 */
	@Override
	public boolean isStartup() {
		return startup;
	}
}
