package org.usf.inspect.core;

import static java.lang.String.join;
import static java.lang.System.getProperty;
import static java.nio.file.Files.createDirectories;
import static java.util.Objects.nonNull;

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
public class DumpProperties {

	private boolean enabled;
	private Path location = Path.of(getProperty("java.io.tmpdir")); // dump folder

	void validate() {
		if(enabled) {
			location = createDirs(location, "inspect");
		}
	}

	static Path createDirs(Path location, String... dirs) {
		var f = location.toFile();
		if(f.exists()) {
			if(nonNull(dirs) && dirs.length > 0) {
				try {
					return createDirectories(f.toPath().resolve(join("/", dirs)));
				} catch (IOException e) {
					throw new IllegalArgumentException("cannot create directories", e);
				}
			}
			return location;
		}
		throw new IllegalArgumentException("dump-directory='" + location + "' no found");
	}
}
