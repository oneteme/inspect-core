package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Metric.prettyDurationFormat;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public abstract class AbstractRequest<T extends AbstractStage> implements Metric {
	
	private String id;
	private String user;
	private Instant start;
	private Instant end;
	private String threadName;
	private String sessionId;
	
	public T createStage(String name, Instant start, Instant end, Throwable t) {
		var stg = createStage();
		stg.setName(name);
		stg.setStart(start);
		stg.setEnd(end);
		if(nonNull(t)) {
			stg.setException(mainCauseException(t));
		}
		stg.setRequestId(id);
		return stg;
	}
	
	protected abstract T createStage();
	
	@Override
	public String toString() {
		return prettyFormat() + " " + prettyDurationFormat(this);
	}
	
	abstract String prettyFormat();
}