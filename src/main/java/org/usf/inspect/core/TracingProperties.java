package org.usf.inspect.core;

import static java.lang.System.getProperty;
import static java.nio.file.Files.createDirectories;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Assertions.assertGreaterOrEquals;
import static org.usf.inspect.core.Assertions.assertPositive;

import java.io.File;
import java.io.IOException;

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
	private String dumpDirectory = getProperty("java.io.tmpdir"); // dump folder
	private RemoteServerProperties remote; //replace server
	
	void validate() {
		dumpDirectory = createDumpDirs(dumpDirectory);
		assertPositive(queueCapacity, "queue-capacity");
		assertGreaterOrEquals(delayIfPending, -1, "dispatch-delay-if-pending");
		if(nonNull(remote)) {
			remote.validate();
		}
	}
	
	static String createDumpDirs(String baseDir) {
		var f = new File(baseDir);
		if(f.exists()) {
			try {
				return createDirectories(f.toPath().resolve("inspect/dump")).toString();
			} catch (IOException e) {
				throw new IllegalArgumentException("cannot create dump directory", e);
			}
		}
		else {
			throw new IllegalArgumentException("dump-directory='" + baseDir + "' no found");
		}
	}
}
