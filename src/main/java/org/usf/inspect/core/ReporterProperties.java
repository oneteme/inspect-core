package org.usf.inspect.core;

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
public final class ReporterProperties {

	private boolean enabled = true;
	private boolean stackTrace = true;

}
