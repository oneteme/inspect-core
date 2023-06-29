package org.usf.traceapi.core;

import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.net.InetAddress.getLocalHost;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;

import java.net.UnknownHostException;
import java.util.List;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Helper {

	static final ThreadLocal<Session> localTrace = new InheritableThreadLocal<>();
	static final Supplier<String> idProvider = ()-> randomUUID().toString();

	static String threadName() {
		return currentThread().getName();
	}
	
	static String operatingSystem() {
		return getProperty("os.name");
	}
	
	static String runtimeEnviroment() {
		return "java " + getProperty("java.version");
	}

	static String hostAddress() {
		try {
			return getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return null;
		}
	}

	static String extractAuthScheme(List<String> authHeaders) { //nullable
		return nonNull(authHeaders) && !authHeaders.isEmpty()
				? extractAuthScheme(authHeaders.get(0)) : null;
	}
	
	static String extractAuthScheme(String authHeader) { //nullable
		return nonNull(authHeader) && authHeader.matches("^\\w+ ") 
				? authHeader.substring(0, authHeader.indexOf(' ')) : null;
	}
}
