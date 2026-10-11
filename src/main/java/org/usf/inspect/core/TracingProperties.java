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
	
	@Deprecated(forRemoval = true, since = "v1.2") //move to Scheduling
	private int queueCapacity = 100_000; //move to SchedulingProperties
	private RemoteServerProperties remote; //replace server
	//v1.1
	@JsonIgnore
	@Deprecated(since = "v1.2", forRemoval = true)
	private DumpProperties dump = new DumpProperties();
	//1.2
	private PublisherType type;
	
	void validate() {
		assertGreaterOrEquals(queueCapacity, 100, "queue-capacity");
		dump.validate();
		if(nonNull(remote)) {
			remote.validate();
		}
	}
}
