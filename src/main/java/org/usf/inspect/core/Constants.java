package org.usf.inspect.core;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {
	
	public static final String REPORTER_NAME 		= "org.usf.inspect.reporter";
	public static final Logger REPORTER 			= getLogger(REPORTER_NAME);
	
	public static final String INSPECT_ENABLED 		= "inspect.collector.enabled";
	//TD use prefix inspect.collector.monitoring
	public static final String WEB_MVC_ENABLED 		= "inspect.collector.monitoring.webmvc.enabled";
	public static final String HTTP_CLIENT_ENABLED 	= "inspect.collector.monitoring.httpclient.enabled";
	public static final String WEB_CLIENT_ENABLED 	= "inspect.collector.monitoring.webclient.enabled";
	public static final String ANNOTATION_ENABLED 	= "inspect.collector.monitoring.annotation.enabled";
	public static final String JDBC_ENABLED 		= "inspect.collector.monitoring.jdbc.enabled";
	public static final String LOGBACK_ENABLED		= "inspect.collector.monitoring.logback.enabled";
	public static final String FLYWAY_ENABLED 		= "inspect.collector.monitoring.flyway.enabled";
	public static final String REACTOR_ENABLED 		= "inspect.collector.monitoring.reactor.enabled";
	public static final String LDAP_ENABLED 		= "inspect.collector.monitoring.ldap.enabled";

	public static final String SCHEDULING_ENABLED 	= "inspect.collector.monitoring.scheduling.enabled";

	public static final String JUNIT4_ENABLED 		= "inspect.collector.monitoring.junit4.enabled";
	public static final String JUNIT5_ENABLED 		= "inspect.collector.monitoring.junit5.enabled";
	public static final String STREAM_ENABLED 		= "inspect.collector.monitoring.stream.enabled";
	public static final String SYSTEM_ENABLED 		= "inspect.collector.monitoring.system.enabled";
	public static final String JSCH_FTP_ENABLED		= "inspect.collector.monitoring.jsch-ftp.enabled";
	public static final String JAKARTA_MAIL_ENABLED = "inspect.collector.monitoring.jakarta-mail.enabled";
	
}
