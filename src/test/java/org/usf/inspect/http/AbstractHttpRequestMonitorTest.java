package org.usf.inspect.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.RequestCommonStatus.*;
import static org.usf.inspect.http.AbstractHttpRequestMonitor.resolveStatus;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.usf.inspect.core.RequestCommonStatus;

class AbstractHttpRequestMonitorTest {

    @Test
    void shouldMapHttpTimeoutException() {
        assertEquals(SERVER_TIMEOUT, resolveStatus(new HttpTimeoutException("timeout")));
    }

    @Test
    void shouldMapSocketTimeoutException() {
        assertEquals(SERVER_TIMEOUT, resolveStatus(new SocketTimeoutException("timeout"))); //TODO : connect | read timeout
    }

    @Test
    void shouldMapTimeoutException() {
        assertEquals(CLIENT_TIMEOUT, resolveStatus(new TimeoutException("timeout")));
    }

    @Test
    void shouldMapInterruptedException() {
        assertEquals(CONN_INTERRUPTED,resolveStatus(new InterruptedException("interrupted")));
    }

    @Test
    void shouldMapSocketException() {
        assertEquals(CONN_ERROR, resolveStatus(new SocketException("connection reset")));
    }

    @Test
    void shouldMapUnknownHostException() {
        assertEquals(CONN_UNKNOWN_HOST, resolveStatus(new UnknownHostException("unknown host")));
    }

    @Test
    void shouldMapUnresolvedAddressException() {
        assertEquals(CONN_UNKNOWN_HOST, resolveStatus(new UnresolvedAddressException()));
    }

    @Test
    void shouldMapUnknownException() {
        assertEquals(SERVER_ERROR, resolveStatus(new RuntimeException("boom")));
    }
}