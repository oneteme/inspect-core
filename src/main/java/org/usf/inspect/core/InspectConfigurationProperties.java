package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.DispatchTarget.REMOTE;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
@ConfigurationProperties(prefix = "inspect")
public final class InspectConfigurationProperties {
	
	private boolean enabled = false;
	private DispatchTarget target = null; //enabled but not dispatching => logging only
	private TrackingProperties track = new TrackingProperties();
	private RestClientProperties server = new RestClientProperties();
	private ScheduledDispatchProperties dispatch = new ScheduledDispatchProperties();
	
	public InspectConfigurationProperties validate() {
		if(enabled) {
			track.validate();
			if(nonNull(target)) {
				dispatch.validate();
				if(target == REMOTE) {
					server.validate();
				}
			}
		}
		return this;
	}
}
