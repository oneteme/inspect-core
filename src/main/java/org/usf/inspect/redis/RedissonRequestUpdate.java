package org.usf.inspect.redis;

import org.usf.inspect.core.AbstractRequestUpdate;

import java.time.Instant;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

public class RedissonRequestUpdate extends AbstractRequestUpdate {

    private boolean failed;

    public RedissonRequestUpdate(String id) {
        super(id);
    }

    public RedissonRequestStage createStage(RedissonAction type, Instant start, Instant end, Throwable thrw, RedisCommand cmd, String... args) {
        if(nonNull(cmd)) {
            setCommand(merge(getCommand(), cmd.getType()));
        }
        if(nonNull(thrw)) {
            failed = true;
        }
        var stg = createStage(type, start, end, cmd, thrw, RedissonRequestStage::new);
        stg.setArgs(args);
        return stg;
    }
}
