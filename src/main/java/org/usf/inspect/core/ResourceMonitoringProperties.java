package org.usf.inspect.core;

import java.io.File;

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

	private boolean enabled;
	private File disk = new File("/");

	void validate() { 
		if(enabled && !disk.exists()) {
			throw new IllegalArgumentException("disk=" + disk + " not found");
		}
	}
}
