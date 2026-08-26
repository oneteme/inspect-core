package org.usf.inspect.core;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author u$f
 *
 */
public interface HasStage {
	
	UUID getId();
	
	AtomicInteger getStageCounter();
}
