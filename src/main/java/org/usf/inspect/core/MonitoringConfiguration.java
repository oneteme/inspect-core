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
	//v1.1
	private ResourceMonitoringProperties resources = new ResourceMonitoringProperties();
	
	void validate() {
		httpRoute.validate();
	}
}
