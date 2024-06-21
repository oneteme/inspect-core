package org.usf.traceapi.core;

import static java.util.Objects.nonNull;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
//@Setter do not use this : setRestSession conflict
public final class TrackingProperties {

	@Setter private boolean jdbcRequest = true;
	@Setter private boolean restRequest = true;
	@Setter private boolean mainSession = true;
	//+ ftp, smtp 
	private RestSessionTrackConfiguration restSession = new RestSessionTrackConfiguration(); //null => false
	
	public void setRestSession(Object o) {
		if(o instanceof Boolean state) {
			restSession = state.booleanValue() ? new RestSessionTrackConfiguration() : null;
		}
		else if(o instanceof RestSessionTrackConfiguration conf) {
			restSession = conf;
		}
		else {
			throw new UnsupportedOperationException("unknown rest-session config : " + String.valueOf(o));
		}
	}

	void validate() {
		if(nonNull(restSession)) {
			restSession.validate();
		}
	}
}
