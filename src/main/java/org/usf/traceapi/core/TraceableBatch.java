package org.usf.traceapi.core;

import static org.usf.traceapi.core.LaunchMode.BATCH;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author u$f
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TraceableStage
public @interface TraceableBatch {
	
	String value() default "";
	
	String location() default "";
	
	LaunchMode type() default BATCH;
	
	/**
	 * require default constructor
	 * 
	 */
	Class<? extends StageUpdater> sessionUpdater() default StageUpdater.class;

}