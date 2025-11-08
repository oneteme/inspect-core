package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties("stages")
public abstract class AbstractRequest implements CompletableMetric {
	
	private Instant start;
	private Instant end;
	private String threadName;
	private String user;
	//v1.1
	private String id;
	private String command;
	private String sessionId;
	private String instanceId; //server usage 
	@JsonIgnore
	private final AtomicInteger stages = new AtomicInteger();
	
	AbstractRequest(AbstractRequest req) {
		this.start = req.start;
		this.end = req.end;
		this.threadName = req.threadName;
		this.user = req.user;
		this.id = req.id;
		this.command = req.command;
		this.sessionId = req.sessionId;
		this.instanceId = req.instanceId;
	}
	
	<T extends AbstractStage> T createStage(Enum<?> type, Instant start, Instant end, Enum<?> command, Throwable t, Supplier<T> supp) {
		assertWasNotCompleted();
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
}