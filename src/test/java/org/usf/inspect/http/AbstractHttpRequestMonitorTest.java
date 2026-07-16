package org.usf.inspect.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.ErrorCode.CONNECTION_UNAVAILABLE;
import static org.usf.inspect.core.ErrorCode.TIMEOUT_OR_INTERRUPTION;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractHttpRequestMonitorTest {

    private AbstractHttpRequestMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new AbstractHttpRequestMonitor();
    }

    @Test
    void shouldMapHttpTimeoutException() {

        int code = monitor.checkException(
                new HttpTimeoutException("timeout"));

        assertEquals(
                TIMEOUT_OR_INTERRUPTION.getCode(),
                code);
    }

    @Test
    void shouldMapSocketTimeoutException() {

        int code = monitor.checkException(
                new SocketTimeoutException("timeout"));

        assertEquals(
                TIMEOUT_OR_INTERRUPTION.getCode(),
                code);
    }

    @Test
    void shouldMapTimeoutException() {

        int code = monitor.checkException(
                new TimeoutException("timeout"));

        assertEquals(
                TIMEOUT_OR_INTERRUPTION.getCode(),
                code);
    }

    @Test
    void shouldMapInterruptedException() {

        int code = monitor.checkException(
                new InterruptedException("interrupted"));

        assertEquals(
                TIMEOUT_OR_INTERRUPTION.getCode(),
                code);
    }

    @Test
    void shouldMapSocketException() {

        int code = monitor.checkException(
                new SocketException("connection reset"));

        assertEquals(
                CONNECTION_UNAVAILABLE.getCode(),
                code);
    }

    @Test
    void shouldMapUnknownHostException() {

        int code = monitor.checkException(
                new UnknownHostException("unknown host"));

        assertEquals(
                CONNECTION_UNAVAILABLE.getCode(),
                code);
    }

    @Test
    void shouldMapUnresolvedAddressException() {

        int code = monitor.checkException(
                new UnresolvedAddressException());

        assertEquals(
                CONNECTION_UNAVAILABLE.getCode(),
                code);
    }

    @Test
    void shouldMapUnknownException() {

        int code = monitor.checkException(
                new RuntimeException("boom"));

        assertEquals(
                UNKNOWN_ERROR.getCode(),
                code);
    }
}