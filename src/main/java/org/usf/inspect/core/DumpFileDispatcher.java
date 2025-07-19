package org.usf.inspect.core;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.delete;
import static java.util.Arrays.stream;
import static org.usf.inspect.core.InspectContext.context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class DumpFileDispatcher {
	
	private static final AtomicInteger COUNTER = new AtomicInteger(0);
	
	private final ObjectMapper mapper;
	private final Path dumpDir;
	private final int id = COUNTER.incrementAndGet(); //unique id for this dispatcher, used in dump file names
	private final Set<String> excludeFiles = new HashSet<>(); //TD save to file, load on start?
	
	public void dump(Collection<?> items) throws IOException {
		var fn = id + "-dump-" + currentTimeMillis() + ".json";
		try {
			mapper.writeValue(dumpDir.resolve(fn).toFile(), items);
			log.debug("dump {} itemps in file {}", items.size(), fn);
		} catch (Exception e) {
			context().reportError("cannot write dump file " + fn, e); //rename
			throw e;
		}
	}
	
	public void forEachDump(Predicate<File> fn) {
		var files = dumpDir.toFile().listFiles(f-> f.getName().matches(id + "-dump-\\d+\\.json"));
		stream(files).filter(f-> !excludeFiles.contains(f.getName())).forEach(f->{
			try {
				if(fn.test(f)){
					skipFile(f);
				}
			} catch (Exception e) {
				context().reportError("cannot read dump file " + f, e); //rename
			}
		});
	}
	
	private void skipFile(File file) {
		boolean done = false;
		try {
			delete(file.toPath());
			done = true; //file deleted
		}
		catch (Exception e) {
			log.warn("cannot delete dump file {}", file, e);
		}
		if(!done) {
			try {
				done = file.renameTo(dumpDir.resolve(file.getName() + ".back").toFile());
			}
			catch (Exception e) {
				log.warn("cannot rename dump file {}", file, e);
			}
		}
		if(!done) {
			excludeFiles.add(file.getName());
			context().reportError("cannot delete or rename dump file " + file.getName());
		}
	}
}
