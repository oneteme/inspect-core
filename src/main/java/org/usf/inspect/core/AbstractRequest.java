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
@JsonIgnoreProperties("stageCounter")
public abstract class AbstractRequest implements CompletableMetric {
	
	private Instant start;
	private Instant end;
	private String threadName;
	private String user;
	//v1.1
	private String id;
	private String sessionId;
	private String instanceId; //server usage 
	@JsonIgnore
	private final AtomicInteger stageCounter = new AtomicInteger();
	
	AbstractRequest(AbstractRequest req) {
		this.user = req.user;
		this.start = req.start;
		this.end = req.end;
		this.threadName = req.threadName;
		this.id = req.id;
		this.sessionId = req.sessionId;
		this.instanceId = req.instanceId;
	}
	
	<T extends AbstractStage> T createStage(Enum<?> type, Instant start, Instant end, Throwable t, Supplier<T> supp) {
		assertNotComplete();
		var stg = supp.get();
		stg.setName(type.name());
		stg.setStart(start);
		stg.setEnd(end);
		if(nonNull(t)) {
			stg.setException(mainCauseException(t));
		}
		stg.setRequestId(id);
		stg.setOrder(stageCounter.getAndIncrement());
		return stg;
	}
}