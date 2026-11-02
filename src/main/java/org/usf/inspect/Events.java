package org.usf.inspect;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Events {

	//log events 0-10
	public static final int LOG = 0; //abstract log event
	public static final int TRACE_ID = 1;
	public static final int DEBUG_ID = 2;
	public static final int INFO_ID  = 3;
	public static final int WARN_ID  = 4;
	public static final int ERROR_ID = 5;
	public static final int ALERT_ID = 6;
}
