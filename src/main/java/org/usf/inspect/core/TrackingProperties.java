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
public final class TrackingProperties {

	private HttpRouteConfiguration httpRoute = new HttpRouteConfiguration(); //replace restSession
	
	void validate() {
		httpRoute.validate();
	}
}
