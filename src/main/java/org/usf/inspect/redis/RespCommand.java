package org.usf.inspect.redis;

public record RespCommand(RedisCommand command, String[] args) {}
