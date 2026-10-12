package org.usf.inspect.core;

import static java.util.Collections.emptySet;

import java.util.Set;

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
public class AnnotationMonitoringProperties {
	
	public static String SPRING_CAHCE = "sprting.cache"; // exclude all cache annotation
	public static String SPRING_SCHEDULE = "sprting.schedule"; //exclude  Scheduled &  Schedules annotation
	public static String SPRING_TRANSACTION = "sprting.tranction"; //exclude Transactional annotation
	public static String INSPECT_TRACEABLE = "inspect.traceable"; //exclude TraceableStage annotation

	private boolean enabled = true;
	private Set<String> excludes = emptySet();
}
