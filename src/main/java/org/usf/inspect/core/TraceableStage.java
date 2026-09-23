package org.usf.inspect.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author u$f
 *
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceableStage {

	/**
	 * Stage name, supports SpEL templates :
	 * <ul>
	 * <li>{@code "import"} : literal, used as is</li>
	 * <li>{@code "import-#{#file.name}"} : {@code #{...}} placeholders are evaluated against method arguments (by name) and target bean</li>
	 * </ul>
	 * Defaults to method name.
	 */
	String name() default "";
}
