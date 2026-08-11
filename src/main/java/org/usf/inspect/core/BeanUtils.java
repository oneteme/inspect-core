package org.usf.inspect.core;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility logging methods for bean loading and wrapping events.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BeanUtils {

	/**
	 * Logs that a bean is being loaded.
	 *
	 * @param name the bean name
	 * @param clazz the bean type
	 */
	public static void logLoadingBean(String name, Class<?> clazz) {
		log.debug("[inspect-beans] loading {} (type: {})..", name, clazz);
	}

	/**
	 * Logs that a bean is being registered.
	 *
	 * @param name the bean name
	 * @param clazz the bean type
	 */
	public static void logRegistringBean(String name, Class<?> clazz) {
		log.info("[inspect-beans] registering {} (type: {})..", name, clazz);
	}

	/**
	 * Logs that a bean is being wrapped.
	 *
	 * @param name the bean name
	 * @param clazz the bean type
	 */
	public static void logWrappingBean(String name, Class<?> clazz) {
		log.info("[inspect-beans] wrapping {} (type: {})..", name, clazz);
	}
}
