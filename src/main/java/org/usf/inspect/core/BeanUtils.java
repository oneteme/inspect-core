package org.usf.inspect.core;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BeanUtils {

	public static void logLoadingBean(String name, Class<?> clazz) {
		log.info("🔄 loading {} (type: {})..", name, clazz);
	}
	
	public static void logRegistringBean(String name, Class<?> clazz) {
		log.info("💾 registering {} (type: {})..", name, clazz);
	}

	public static void logWrappingBean(String name, Class<?> clazz) {
		log.info("📦 wrapping {} (type: {})..", name, clazz);
	}
	
	public static void logRegistringHook(String name, String key) {
		log.info("💾 registering {} (key: {})..", name, key);
	}
	
	public static void logRemovinggHook(String name, String key) {
		log.info("🗑️ removing {} (key: {})..", name, key);
	}
}
