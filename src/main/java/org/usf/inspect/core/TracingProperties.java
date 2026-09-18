package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Assertions.assertGreaterOrEquals;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
	
	private int queueCapacity = 100_000; // {n} max buffering traces, min=100
	private RemoteServerProperties remote; //replace server
	//v1.1
	@JsonIgnore
	@Deprecated(since = "v1.2", forRemoval = true)
	private DumpProperties dump = new DumpProperties();
	
	void validate() {
		assertGreaterOrEquals(queueCapacity, 10, "queue-capacity");
		dump.validate();
		if(nonNull(remote)) {
			remote.validate();
		}
	}
}
