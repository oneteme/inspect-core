package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Provides helper methods for traces that create ordered stages.
 */
public interface HasStage {
	
	/**
	 * Returns the request identifier associated with the stages.
	 *
	 * @return the request identifier
	 */
	String getId();
	
	/**
	 * Returns the stage counter used to order created stages.
	 *
	 * @return the stage counter
	 */
	AtomicInteger getStageCounter();

	/**
	 * Creates and initializes a stage instance.
	 *
	 * @param <T> the stage type
	 * @param type the stage action type
	 * @param start the stage start time
	 * @param end the stage end time
	 * @param command the command associated with the stage
	 * @param thrw the failure cause, if any
	 * @param supp the stage supplier
	 * @return the initialized stage
	 */
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
