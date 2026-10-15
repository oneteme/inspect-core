package org.usf.inspect.config;

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
public final class ReportingProperties {

	private boolean enabled = true;
	private boolean stackTrace = true;

}
