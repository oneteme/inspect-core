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
public final class MonitoringConfiguration {

	private HttpRouteMonitoringProperties httpRoute = new HttpRouteMonitoringProperties(); //replace restSession
	private ResourceMonitoringProperties resources = new ResourceMonitoringProperties();
	private ExceptionMonitoringConfiguration exception = new ExceptionMonitoringConfiguration();
	//v1.2
	private LogbackMonitoringProperties logback = new LogbackMonitoringProperties();
	
	void validate() {
		httpRoute.validate();
		resources.validate();
		exception.validate();
		logback.validate();
	}
}
