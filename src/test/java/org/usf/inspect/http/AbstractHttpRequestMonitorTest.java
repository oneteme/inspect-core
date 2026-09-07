package org.usf.inspect.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.DualEventTracer.CONN_ERROR;
import static org.usf.inspect.core.DualEventTracer.CONN_INTERRUPTED;
import static org.usf.inspect.core.DualEventTracer.CONN_UNKNOWN_HOST;
import static org.usf.inspect.core.DualEventTracer.SERVER_ERROR;
import static org.usf.inspect.core.DualEventTracer.SERVER_TIMEOUT;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class AbstractHttpRequestMonitorTest {

	HttpConnectionLifecycleTracer listener = new HttpConnectionLifecycleTracer();
	
    @Test
    void shouldMapHttpTimeoutException() {
        assertEquals(SERVER_TIMEOUT, listener.resolveStatus(new HttpTimeoutException("timeout")));
    }

    @Test
    void shouldMapSocketTimeoutException() {
        assertEquals(SERVER_TIMEOUT, listener.resolveStatus(new SocketTimeoutException("timeout"))); //TODO : connect | read timeout
    }

    @Test
    void shouldMapTimeoutException() {
        assertEquals(CONN_INTERRUPTED, listener.resolveStatus(new TimeoutException("timeout")));
    }

    @Test
    void shouldMapInterruptedException() {
        assertEquals(CONN_INTERRUPTED,listener.resolveStatus(new InterruptedException("interrupted")));
    }

    @Test
    void shouldMapSocketException() {
        assertEquals(CONN_ERROR, listener.resolveStatus(new SocketException("connection reset")));
    }

    @Test
    void shouldMapUnknownHostException() {
        assertEquals(CONN_UNKNOWN_HOST, listener.resolveStatus(new UnknownHostException("unknown host")));
    }

    @Test
    void shouldMapUnresolvedAddressException() {
        assertEquals(CONN_UNKNOWN_HOST, listener.resolveStatus(new UnresolvedAddressException()));
    }

    @Test
    void shouldMapUnknownException() {
        assertEquals(SERVER_ERROR, listener.resolveStatus(new RuntimeException("boom")));
    }
}