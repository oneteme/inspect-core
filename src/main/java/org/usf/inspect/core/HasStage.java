package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 
 * @author u$f
 *
 */
public interface HasStage {
	
	String getId();
	
	AtomicInteger getStageCounter();

	default <T extends AbstractStage> T createStage(Enum<?> type, Instant start, Instant end, Enum<?> command, Throwable thrw, Supplier<T> supp) {
		var idx = getStageCounter().getAndIncrement();
		var stg = supp.get();
		stg.setName(type.name());
		stg.setStart(start);
		stg.setEnd(end);
		if(nonNull(command)) {
			stg.setCommand(command.name());
		}
		if(nonNull(thrw)) {
			stg.setException(mainCauseException(thrw));
		}
		stg.setRequestId(getId());
		stg.setOrder(idx);
		return stg;
	}
}
