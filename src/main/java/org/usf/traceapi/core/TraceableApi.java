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
	
	String group() default "";
	
	String[] endpoint() default {};

	String[] resource() default {};
	
	Class<? extends ClientProvider> clientProvider() default ClientProvider.class; //require no args constructor
	
	//boolean enabled() default true;
}