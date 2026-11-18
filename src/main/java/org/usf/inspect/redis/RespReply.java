package org.usf.inspect.redis;

public record RespReply(RespType type, Object value) {

    public enum RespType {
        SIMPLE_STRING, ERROR, INTEGER, BULK_STRING, ARRAY, NULL
    }
}
