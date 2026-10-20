package org.usf.inspect.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.usf.inspect.test.Junit5TestMonitor;

/**
 * 
 * @author u$f
 *
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(Junit5TestMonitor.class)
public @interface DisabledConfiguration {

}
