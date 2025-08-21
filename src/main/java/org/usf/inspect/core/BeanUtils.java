package org.usf.inspect.core;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BeanUtils {

	public static void logLoadingBean(String name, Class<?> clazz) {
		log.info("[inspect-monitoring] loading {} (type: {})..", name, clazz);
	}

	public static void logRegistringBean(String name, Class<?> clazz) {
		log.info("[inspect-monitoring] registering {} (type: {})..", name, clazz);
	}

	public static void logWrappingBean(String name, Class<?> clazz) {
		log.info("[inspect-monitoring] wrapping {} (type: {})..", name, clazz);
	}
}
