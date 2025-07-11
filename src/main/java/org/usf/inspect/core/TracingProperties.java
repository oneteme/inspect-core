package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Assertions.assertGreaterOrEquals;
import static org.usf.inspect.core.Assertions.assertMatches;
import static org.usf.inspect.core.Assertions.assertPositive;

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
public class TracingProperties { //add remote
	
	private int queueCapacity = 10_000; // {n} max buffering traces, 0: unlimited
	//v1.1
	private int delayIfPending = 30; // send pending traces after {n} seconds, 0: send immediately, -1 not 
	private String dumpDirectory = "/tmp"; // dump folder
	private RemoteServerProperties remote; //replace server
	
	void validate() {
		assertPositive(queueCapacity, "queue-capacity");
		assertGreaterOrEquals(delayIfPending, -1, "dispatch-delay-if-pending");
		assertMatches(dumpDirectory, "(\\/[\\w-]+)+", "dumpDir");
		if(nonNull(remote)) {
			remote.validate();
		}
	}
}
