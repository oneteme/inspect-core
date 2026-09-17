package org.usf.inspect.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.DualEventTracer.CNX_ERROR;
import static org.usf.inspect.core.DualEventTracer.CNX_INTERRUPTED;
import static org.usf.inspect.core.DualEventTracer.CNX_UNKNOWN_HOST;
import static org.usf.inspect.core.DualEventTracer.RMT_ERROR;
import static org.usf.inspect.core.DualEventTracer.RMT_TIMEOUT;

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
        assertEquals(RMT_TIMEOUT, listener.resolveStatus(new HttpTimeoutException("timeout")));
    }

    @Test
    void shouldMapSocketTimeoutException() {
        assertEquals(RMT_TIMEOUT, listener.resolveStatus(new SocketTimeoutException("timeout"))); //TODO : connect | read timeout
    }

    @Test
    void shouldMapTimeoutException() {
        assertEquals(CNX_INTERRUPTED, listener.resolveStatus(new TimeoutException("timeout")));
    }

    @Test
    void shouldMapInterruptedException() {
        assertEquals(CNX_INTERRUPTED,listener.resolveStatus(new InterruptedException("interrupted")));
    }

    @Test
    void shouldMapSocketException() {
        assertEquals(CNX_ERROR, listener.resolveStatus(new SocketException("connection reset")));
    }

    @Test
    void shouldMapUnknownHostException() {
        assertEquals(CNX_UNKNOWN_HOST, listener.resolveStatus(new UnknownHostException("unknown host")));
    }

    @Test
    void shouldMapUnresolvedAddressException() {
        assertEquals(CNX_UNKNOWN_HOST, listener.resolveStatus(new UnresolvedAddressException()));
    }

    @Test
    void shouldMapUnknownException() {
        assertEquals(RMT_ERROR, listener.resolveStatus(new RuntimeException("boom")));
    }
}