package org.usf.inspect.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or annotation as a traceable stage that should be monitored and recorded by the inspect framework.
 *
 * @author u$f
 *
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceableStage {

	/**
	 * Optional SpEL expression that resolves the stage name from the method context.
	 * When empty, the method name is used as the stage name.
	 *
	 * @return the stage name expression
	 */
	String name() default "";
}
