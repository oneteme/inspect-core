package org.usf.traceapi.core;

import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.DispatchMode.REMOTE;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "inspect")
public final class InspectConfigurationProperties {
	
	private boolean enabled = false;
	private DispatchMode mode = null; //enabled but not dispatching => logging only
	private TrackingProperties track = new TrackingProperties();
	private RemoteTracerProperties server = new RemoteTracerProperties();
	private ScheduledDispatchProperties dispatch = new ScheduledDispatchProperties();
	
	public InspectConfigurationProperties validate() {
		if(enabled) {
			track.validate();
			if(nonNull(mode)) {
				dispatch.validate();
				if(mode == REMOTE) {
					server.validate();
				}
			}
		}
		return this;
	}
}
