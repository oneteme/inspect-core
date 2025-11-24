package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorReporter.reporter;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@RequiredArgsConstructor
public class AbstractRequestCallback implements EventTrace {

	@JsonIgnore
	private final AtomicInteger stages = new AtomicInteger();
	private final String id;
	
	private String command;
	private Instant end;
	
	public boolean assertStillConnected(){
		if(nonNull(command)) {
			reporter().action("assertStillConnected").trace(this).emit();
			return false;
		}
		return false;
	}
	
	<T extends AbstractStage> T createStage(Enum<?> type, Instant start, Instant end, Enum<?> command, Throwable t, Supplier<T> supp) {
		var idx = stages.getAndIncrement();
		var stg = supp.get();
		stg.setName(type.name());
		stg.setStart(start);
		stg.setEnd(end);
		if(nonNull(command)) {
			stg.setCommand(command.name());
		}
		if(nonNull(t)) {
			stg.setException(mainCauseException(t));
		}
		stg.setRequestId(id);
		stg.setOrder(idx);
		return stg;
	}
	
	public static void reportNoActiveRequest(String action, RequestMask mask) {
		reporter().action(action).message("no active request["+mask+"]").thread().emit();
	}
}
