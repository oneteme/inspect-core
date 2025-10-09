package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Assertions.assertGreaterOrEquals;

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
	
	private int queueCapacity = 10_000; // {n} max buffering traces, min=100
	private RemoteServerProperties remote; //replace server
	//v1.1
	private boolean modifiable = true; //if true dispatch a trace copy
	private int delayIfPending = 30; // send pending traces after {n} seconds, 0: send immediately, -1 not 
	private DumpProperties dump = new DumpProperties();
	private PurgeProperties purge = new PurgeProperties();
	
	void validate() {
		assertGreaterOrEquals(queueCapacity, 100, "queue-capacity");
		assertGreaterOrEquals(delayIfPending, -1, "dispatch-if-pending");
		dump.validate();
		if(nonNull(remote)) {
			remote.validate();
		}
	}
}
