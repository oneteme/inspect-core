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
		log.debug("[inspect-beans] loading {} (type: {})..", name, clazz);
	}

	public static void logRegistringBean(String name, Class<?> clazz) {
		log.info("[inspect-beans] registering {} (type: {})..", name, clazz);
	}

	public static void logWrappingBean(String name, Class<?> clazz) {
		log.info("[inspect-beans] wrapping {} (type: {})..", name, clazz);
	}
}
