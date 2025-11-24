package org.usf.inspect.core;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class HttpSessionCallback extends AbstractSessionCallback {

	public HttpSessionCallback(String id) {
		super(id);
	}
}
