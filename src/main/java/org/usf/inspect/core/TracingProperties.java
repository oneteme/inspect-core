package org.usf.inspect.core;

import static org.usf.inspect.core.Assertions.assertStrictPositive;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@ToString
public class TracingProperties {

	private int retentionMaxAge = 30; //
	
	void validate() {
		assertStrictPositive(retentionMaxAge, "retentionMaxAge");
	}
}
