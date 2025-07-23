package org.usf.inspect.core;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.move;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static org.usf.inspect.core.InspectContext.context;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
public final class EventTraceDumper implements DispatchHook {
	
	private final Set<String> excludeFiles = new HashSet<>(); //TD save to file, load on start?
	private final Path dumpDir;
	private final ObjectMapper mapper;
	
	@Override
	public void preDispatch(Dispatcher dispatcher) {
		stream(listDumpFiles()).forEach(f->{
			var it = getFileDispatchAttempts(f)+1;
			dispatcher.dispatchNow(f, it, (v, t)->{
				if(isNull(t)) {
					deleteFile(f); //cannot throw exception 
				}
				else {
					updateFileDispatchAttempts(f, it);
				}
			});
		});
	}
	
	@Override
	public void postDispatch(Dispatcher dispatcher) {
		dispatcher.tryPropagateQueue(dispatcher.getState().wasCompleted() ? 0 : -1, (arr, max)-> { //excludes all pending metrics
			dumpTraces(arr);
			return emptyList();
		});
	}
	
	String dumpTraces(Collection<EventTrace> traces) {
		var fn = "dump_" + currentTimeMillis() + ".json";
		try {
			mapper.writeValue(dumpDir.resolve(fn).toFile(), traces);
			log.debug("{} traces was dumped in {}", traces.size(), fn);
			return fn;
		}
		catch (Exception e) {
			throw new DispatchException("creating dump file " + fn + " error", e);
		}
	}
	
	File[] listDumpFiles() {
		return dumpDir.toFile().listFiles(f-> 
			!excludeFiles.contains(f.getName())
			&& f.getName().matches("dump_\\d+\\.json(~\\d+)?"));
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
	
	static void updateFileDispatchAttempts(File file, int attempts) {
		try {
			var fn = file.getName();
			var idx = fn.indexOf('~');
			if(idx > -1) {
				fn = fn.substring(0, idx);
			}
			var path = file.toPath();
			move(path, path.getParent().resolve(fn+"~"+attempts));
		}
		catch (Exception e) {
			//ignore it
		}
	}
	
	static int getFileDispatchAttempts(File file) {
		try {
			var fn = file.getName();
			var idx = fn.indexOf('~')+1;
			return idx > -1 ? parseInt(fn.substring(idx)) : 0;
		}
		catch (Exception e) {
			//ignore it
			return 0;
		}
	}
}
