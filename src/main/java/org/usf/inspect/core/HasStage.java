package org.usf.inspect.core;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author u$f
 *
 */
public interface HasStage {
	
	String getId();
	
	AtomicInteger getStageCounter();
}
