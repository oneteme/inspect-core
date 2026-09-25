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
public class InspectCollectorConfiguration {
	
	private boolean enabled = false;
	private boolean debugMode = false; // enable debug mode, e.g. for testing
	private SchedulingProperties scheduling = new SchedulingProperties(); //replace dispatch
	private MonitoringConfiguration monitoring = new MonitoringConfiguration();
	private TracingProperties tracing = new TracingProperties();
	
	public void validate() {
		if(enabled) {
			scheduling.validate();
			monitoring.validate();
			tracing.validate();
		}
	}
	
	static InspectCollectorConfiguration initializeConfiguration(boolean enable) {
		var config = new InspectCollectorConfiguration();
		config.setEnabled(enable);
		return config;
	}
}
