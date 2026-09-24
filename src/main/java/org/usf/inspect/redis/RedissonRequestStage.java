package org.usf.inspect.redis;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.AbstractStage;
import org.usf.inspect.core.EventTraceFormatter;

@Getter
@Setter
public final class RedissonRequestStage extends AbstractStage {

    private String[] args;

    @Override
    public String toString() {
        return new EventTraceFormatter()
                .withAction(getName())
                .withArgsAsTopic(getCommand(), args)
                .withPeriod(getStart(), getEnd())
                .withResult(getException())
                .format();
    }
}
