package org.usf.inspect;

import java.util.UUID;

/**
 * 
 * @author u$f
 *
 */
public interface SessionTrace {

	UUID getId();
	
	int getEventMask();
	
	void setEventMask(int mask);

}
