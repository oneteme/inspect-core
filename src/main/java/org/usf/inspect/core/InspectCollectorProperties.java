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
public class InspectCollectorProperties {
	
	private boolean enabled = false;
	private boolean debugMode = false; // enable debug mode, e.g. for testing
	private SchedulingProperties scheduling = new SchedulingProperties(); //replace dispatch
	private MonitoringProperties monitoring = new MonitoringProperties();
	private TracingProperties tracing = new TracingProperties();
	//v1.2
	private ReporterProperties reporting = new ReporterProperties();
	
	public void validate() {
		if(enabled) {
			scheduling.validate();
			monitoring.validate();
			tracing.validate();
		}
	}
	
	static InspectCollectorProperties initializeConfiguration(boolean enable) {
		var config = new InspectCollectorProperties();
		config.setEnabled(enable);
		return config;
	}
}
