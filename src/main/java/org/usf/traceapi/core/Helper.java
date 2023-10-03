package org.usf.traceapi.core;

import static java.lang.Thread.currentThread;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Helper {
	
	static final Logger log = getLogger(Helper.class.getPackage().getName() + ".TraceAPI");
	
	static String basePackage = "fr.enedis";

	static final ThreadLocal<Session> localTrace = new InheritableThreadLocal<>();
	
	static ApplicationInfo application; //unsafe set
	
	static ApplicationInfo applicationInfo() {
		return application;
	}
	
	static String threadName() {
		return currentThread().getName();
	}
		
	static String extractAuthScheme(List<String> authHeaders) { //nullable
		return nonNull(authHeaders) && authHeaders.size() == 1 //require one header
				? extractAuthScheme(authHeaders.get(0)) : null;
	}
	
	static String extractAuthScheme(String authHeader) { //nullable
		return nonNull(authHeader) && authHeader.matches("\\w+ .+") 
				? authHeader.substring(0, authHeader.indexOf(' ')) : null;
	}
	
	static <T> Optional<T> newInstance(Class<? extends T> clazz) {
		try {
			return Optional.of(clazz.getDeclaredConstructor().newInstance());
		} catch (Exception e) {
			log.warn("cannot instantiate class " + clazz.getName(), e);
			return empty();
		}
	}
	
	//TODO rename
	static Optional<StackTraceElement> stackTraceElement() {
		if(isNull(basePackage) || basePackage.isBlank()) {
			return empty();
		}
		var arr = currentThread().getStackTrace();
		var i = 1; //location, internal call
		while (++i<arr.length && !arr[i].getClassName().startsWith(basePackage));
		return i<arr.length ? Optional.of(arr[i]) : empty(); 
	}
	
}
