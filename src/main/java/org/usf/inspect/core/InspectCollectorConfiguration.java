package org.usf.inspect.core;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Defines the top-level configuration options for the inspect collector.
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@ToString
public class InspectCollectorConfiguration {
	
	private boolean enabled = false;
	private SchedulingProperties scheduling = new SchedulingProperties(); //replace dispatch
	private MonitoringConfiguration monitoring = new MonitoringConfiguration();
	private TracingProperties tracing = new TracingProperties();
	//v1.1
	private boolean debugMode = false; // enable debug mode, e.g. for testing
	
	/**
	 * Validates the nested configuration when the collector is enabled.
	 * 
	 * @return this configuration instance
	 */
	public InspectCollectorConfiguration validate() {
		if(enabled) {
			scheduling.validate();
			monitoring.validate();
			tracing.validate();
		}
		return this;
	}
}
