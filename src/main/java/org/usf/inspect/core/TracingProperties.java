package org.usf.inspect.core;

import static java.lang.String.join;
import static java.lang.System.getProperty;
import static java.nio.file.Files.createDirectories;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Assertions.assertGreaterOrEquals;

import java.io.IOException;
import java.nio.file.Path;

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
	//v1.1
	private int delayIfPending = 30; // send pending traces after {n} seconds, 0: send immediately, -1 not 
	private Path dumpDirectory = Path.of(getProperty("java.io.tmpdir")); // dump folder
	private RemoteServerProperties remote; //replace server
	
	void validate() {
		dumpDirectory = createDirs(dumpDirectory, "inspect");
		assertGreaterOrEquals(queueCapacity, 100, "queue-capacity");
		assertGreaterOrEquals(delayIfPending, -1, "dispatch-delay-if-pending");
		if(nonNull(remote)) {
			remote.validate();
		}
	}
	
	static Path createDirs(Path baseDir, String... dirs) {
		var f = baseDir.toFile();
		if(f.exists()) {
			if(nonNull(dirs) && dirs.length > 0) {
				try {
					return createDirectories(f.toPath().resolve(join("/", dirs)));
				} catch (IOException e) {
					throw new IllegalArgumentException("cannot create directories", e);
				}
			}
			return baseDir;
		}
		throw new IllegalArgumentException("dump-directory='" + baseDir + "' no found");
	}
}
