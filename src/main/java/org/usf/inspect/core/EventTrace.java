package org.usf.inspect.core;

import static org.usf.inspect.core.ErrorReporter.stackReporter;
import static org.usf.inspect.core.InspectContext.context;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 
 * @author u$f
 *
 */
@JsonTypeInfo(
	    use = JsonTypeInfo.Id.NAME,
	    include = JsonTypeInfo.As.PROPERTY,
	    property = "@type")
public interface EventTrace { 
	
	default void emit() {
		try {
			context().emitTrace(this);
		}
		catch (Throwable ex) {// do not throw exception
			stackReporter().action("EventTrace.emit").cause(ex).emit();
		}
	}
}