package org.usf.inspect;

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
public final class ReportingProperties {

	private boolean enabled = true;
	private boolean errorStackTrace = true;
	private boolean runtimeStackTrace = true;

}
