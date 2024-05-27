package org.usf.traceapi.core;

import static org.usf.traceapi.core.MainSessionType.BATCH;

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

	String value() default ""; // stage name
	
	MainSessionType type() default BATCH; // only for main sessions

	/**
	 * require default constructor
	 * 
	 */
	Class<? extends StageUpdater> sessionUpdater() default StageUpdater.class;
	
	//boolean enabled() default true
	
}
