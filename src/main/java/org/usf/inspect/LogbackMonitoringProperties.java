package org.usf.inspect;

import static java.util.Objects.isNull;
import static org.usf.inspect.LogLevel.WARN;

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
public class LogbackMonitoringProperties {

	private boolean enabled = true;
	private LogLevel level = WARN;
	private String rootPackage;
	
	void validate() {
		if(enabled) {
			if(isNull(level)) {
				level = WARN;
			}
			if(isNull(rootPackage) || rootPackage.isEmpty()) {
				rootPackage = "ROOT"; //org.slf4j.Logger.ROOT_LOGGER_NAME
			}
			else if(!rootPackage.matches("\\w+(\\.\\w+)*")) {
				throw new IllegalArgumentException("illegal package name="+rootPackage);
			}
		}
	}
}
