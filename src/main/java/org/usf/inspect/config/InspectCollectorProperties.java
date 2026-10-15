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
public class InspectCollectorProperties {
	
	private boolean enabled = false;
	@Getter
	private boolean debugMode = false; // enable debug mode, e.g. for testing
	private SchedulingProperties scheduling = new SchedulingProperties();
	private MonitoringProperties monitoring = new MonitoringProperties();
	private TracingProperties tracing = new TracingProperties();
	//v1.2
	private ReportingProperties reporting = new ReportingProperties();
	
	public void validate() {
		if(enabled) {
			scheduling.validate();
			monitoring.validate();
			tracing.validate();
		}
	}
	
	public static InspectCollectorProperties initializeConfiguration(boolean enable) {
		var config = new InspectCollectorProperties();
		config.setEnabled(enable);
		return config;
	}
}
