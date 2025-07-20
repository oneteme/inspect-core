package org.usf.inspect.core;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.delete;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static org.usf.inspect.core.InspectContext.context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.usf.inspect.core.Dispatcher.DispatchHook;

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
public final class ObjectDumper implements DispatchHook {
	
	private static final AtomicInteger COUNTER = new AtomicInteger(0);
	
	private final ObjectMapper mapper;
	private final Path dumpDir;
	private final int id = COUNTER.incrementAndGet(); //unique id for this dispatcher, used in dump file names
	private final Set<String> excludeFiles = new HashSet<>(); //TD save to file, load on start?
	
	@Override
	public void preDispatch(Dispatcher dispatcher) {
		stream(dumpFiles()).forEach(f->{
			if(dispatcher.dispatch(f)) {
				deleteFile(f);
			}
		});
	}
	
	@Override
	public void postDispatch(Dispatcher dispatcher) {
		dispatcher.tryReduceQueue(-1, (trc, max)-> { //excludes all pending metrics
			try {
				dump(trc);
			}
			catch (Exception e) {
				return trc;
			}
			return emptyList();
		});
	}
	
	String dump(Object o) throws IOException {
		var fn = id + "-dump-" + currentTimeMillis() + ".json";
		mapper.writeValue(dumpDir.resolve(fn).toFile(), o);
		log.debug("dump file {} created", fn);
		return fn;
	}
	
	
	File[] dumpFiles() {
		return dumpDir.toFile()
				.listFiles(f-> f.getName().matches(id + "-dump-\\d+\\.json")
						&& !excludeFiles.contains(f.getName()));
	}
	
	void deleteFile(File file) {
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
