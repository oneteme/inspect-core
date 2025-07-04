package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Metric.prettyDurationFormat;

import java.time.Instant;
import java.util.function.Supplier;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public abstract class AbstractRequest implements LazyMetric {
	
	private String user;
	private Instant start;
	private Instant end;
	private String threadName;
	private String sessionId;
	//v1.1
	private String id;
	
	public <T extends AbstractStage> T createStage(String name, Instant start, Instant end, Throwable t, Supplier<T> supp) {
		var stg = supp.get();
		stg.setName(name);
		stg.setStart(start);
		stg.setEnd(end);
		if(nonNull(t)) {
			stg.setException(mainCauseException(t));
		}
		stg.setRequestId(id);
		return stg;
	}
	
	@Override
	public String toString() {
		return prettyFormat() + " " + prettyDurationFormat(this);
	}
	
	abstract String prettyFormat();
}