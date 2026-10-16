package org.usf.inspect;

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

	private HttpRouteMonitoringProperties webmvc = new HttpRouteMonitoringProperties();
	private SystemMonitoringProperties system = new SystemMonitoringProperties();
	private ExceptionMonitoringProperties exception = new ExceptionMonitoringProperties();
	//v1.2
	private LogbackMonitoringProperties logback = new LogbackMonitoringProperties();
	private AnnotationMonitoringProperties annotation = new AnnotationMonitoringProperties();
	
	private DefaultMonitoringProperties scheduling = new DefaultMonitoringProperties();
	private DefaultMonitoringProperties stream = new DefaultMonitoringProperties();
	private DefaultMonitoringProperties jdbc = new DefaultMonitoringProperties();
	private DefaultMonitoringProperties ldap = new DefaultMonitoringProperties();
	
	private DefaultMonitoringProperties jschFtp = new DefaultMonitoringProperties();
	private DefaultMonitoringProperties jakartaMail = new DefaultMonitoringProperties();
	private DefaultMonitoringProperties httpclient = new DefaultMonitoringProperties();
	private DefaultMonitoringProperties webclient = new DefaultMonitoringProperties();
	private DefaultMonitoringProperties reactor = new DefaultMonitoringProperties();
	private DefaultMonitoringProperties flyway = new DefaultMonitoringProperties();
	private DefaultMonitoringProperties junit4 = new DefaultMonitoringProperties();
	private DefaultMonitoringProperties junit5 = new DefaultMonitoringProperties();
	
	void validate() {
		webmvc.validate();
		system.validate();
		exception.validate();
		logback.validate();
	}
}
