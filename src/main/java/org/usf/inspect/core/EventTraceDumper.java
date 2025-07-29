package org.usf.inspect.core;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.move;
import static org.usf.inspect.core.InspectContext.context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

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
	
	private final Path baseDir;
	private final ObjectMapper mapper;
	
	@Override
	public boolean onCapacityExceeded(EventTrace[] traces) {
		var fn = "dump_" + currentTimeMillis() + ".json";
		var f = baseDir.resolve(fn).toFile(); //can write !?
		try {
			mapper.writeValue(f, traces);
			log.debug("{} traces was dumped in '{}' file", traces.length, fn);
		}
		catch (IOException e) {
			throw new DispatchException("creating traces dump file '" + fn + "' error", e);
		}
		emitDispatchFileTask(f);
		return true;
	}
	
	static void emitDispatchFileTask(File f) {
		var fileRef = new File[] {f};
		context().emitTask(agn->{
			if(fileRef[0].exists()) {
				var rt = getFileDispatchAttempts(fileRef[0].getName());
				try {
					agn.dispatch(++rt, fileRef[0]);
					deleteFile(fileRef[0]); //ignore deleteFile exception
					if(rt > 5) { //more than one attempt
						log.info("successfully dispatched '{}' file after {} attempts", fileRef[0].getName(), rt);
					}
				}
				catch (Exception e) {
					fileRef[0] = setFileDispatchAttempts(fileRef[0], rt);
					throw e;
				}
			}
			else { //do not throw exception => end task
				context().reportError("traces dump file '" + f.getName() + "' is not found"); 
			}
		});
	}
	
	static void deleteFile(File file) {
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
				done = file.renameTo(file.toPath().resolveSibling(file.getName() + ".back").toFile());
			}
			catch (Exception e) {
				log.warn("cannot rename dump file {}", file, e);
			}
		}
		if(!done) {
			context().reportError("cannot delete or rename file '" + file.getName() + "'"); 
		}
	}
	
	static File setFileDispatchAttempts(File file, int attempts) {
		try {
			var fn = file.getName();
			var idx = fn.indexOf('~');
			if(idx > -1) {
				fn = fn.substring(0, idx);
			}
			var path = file.toPath();
			return move(path, path.resolveSibling(fn+"~"+attempts)).toFile();
		}
		catch (Exception e) {//cannot move file
			return file;
		}
	}
	
	static int getFileDispatchAttempts(String fn) {
		try {
			var idx = fn.indexOf('~');
			return idx > 0 ? parseInt(fn.substring(++idx)) : 0;
		}
		catch (Exception e) { //ignore it
			return 0;
		}
	}
}
