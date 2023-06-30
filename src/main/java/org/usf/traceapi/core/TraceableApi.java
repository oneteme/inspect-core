package org.usf.traceapi.core;

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
public @interface TraceableApi {

	String value() default "";

	/**
	 * require default constructor
	 * 
	 */
	Class<? extends ApiUserProvider> clientProvider() default DefaultUserProvider.class;
	
	//boolean enabled() default true
}