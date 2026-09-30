package org.usf.inspect.redis;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.AbstractRequestSignal;

import java.time.Instant;

@Getter
@Setter
public class RedissonRequestSignal extends AbstractRequestSignal {

    private String host;
    private int port;

    public RedissonRequestSignal(String id, String sessionId, Instant start, String threadName) {
        super(id, sessionId, start, threadName);
    }

    public RedissonRequestUpdate createCallback() {
        return new RedissonRequestUpdate(getId());
    }
}
