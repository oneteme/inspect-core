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
public class MainSessionCallback extends AbstractSessionCallback {

	private Instant start; //updated sometime after initialization

	@JsonCreator
	public MainSessionCallback(String id) {
		super(id);
	}
}
