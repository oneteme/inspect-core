package org.usf.inspect.core;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@ToString
public class ResourceMonitoringProperties {

	private boolean enabled = true;

	void validate() { 
		//noting to validate
	}
}
