package org.usf.inspect.core;

import static java.util.Objects.nonNull;

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
public final class InspectCollectorConfiguration {
	
	private boolean enabled = false;
	private TrackingProperties tracking = new TrackingProperties();
	private SchedulingProperties scheduling = new SchedulingProperties();//replace dispatch
	private DispatchingProperties dispatching; //replace server
	//v1.1
	private boolean debugMode = false; // enable debug mode, e.g. for testing
	
	public InspectCollectorConfiguration validate() {
		if(enabled) {
			tracking.validate();
			scheduling.validate();
			if(nonNull(dispatching)) {
				dispatching.validate();
			}
		}
		return this;
	}
}
