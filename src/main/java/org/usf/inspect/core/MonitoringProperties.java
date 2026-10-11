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
public final class MonitoringProperties {

	private HttpRouteMonitoringProperties httpRoute = new HttpRouteMonitoringProperties(); //replace restSession
	private SystemMonitoringProperties system = new SystemMonitoringProperties();
	private ExceptionMonitoringProperties exception = new ExceptionMonitoringProperties();
	//v1.2
	private LogbackMonitoringProperties logback = new LogbackMonitoringProperties();
	private AnnotationMonitoringProperties annotation = new AnnotationMonitoringProperties();
	
	void validate() {
		httpRoute.validate();
		system.validate();
		exception.validate();
		logback.validate();
	}
}
