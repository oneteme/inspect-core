package org.usf.inspect.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.DualEventTracer.CNX_ERROR;
import static org.usf.inspect.core.DualEventTracer.CNX_INTERRUPTED;
import static org.usf.inspect.core.DualEventTracer.CNX_REFUSED;
import static org.usf.inspect.core.DualEventTracer.CNX_UNKNOWN_HOST;
import static org.usf.inspect.core.DualEventTracer.RMT_ERROR;
import static org.usf.inspect.core.DualEventTracer.RMT_TIMEOUT;

import java.io.EOFException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;

import org.junit.jupiter.api.Test;

class DatabaseRequestMonitorTest {

	private final DatabaseConnectionLifecycleTracer listener = new DatabaseConnectionLifecycleTracer(null);

    @Test
    void shouldMapSqlTimeoutException() {
        assertEquals(RMT_TIMEOUT, listener.resolveStatus(new SQLTimeoutException()));
    }

    @Test
    void shouldMapSocketTimeoutException() {
        assertEquals(RMT_TIMEOUT, listener.resolveStatus(new SocketTimeoutException()));
    }

    @Test
    void shouldMapInterruptedException() {
        assertEquals(CNX_INTERRUPTED, listener.resolveStatus(new InterruptedException()));
    }

    @Test
    void shouldMapEofException() {
        assertEquals(CNX_ERROR, listener.resolveStatus(new EOFException()));
    }

    @Test
    void shouldMapUnknownHostException() {
        assertEquals(CNX_UNKNOWN_HOST, listener.resolveStatus(new UnknownHostException()));
    }

    @Test
    void shouldMapSocketException() {
        assertEquals(CNX_ERROR, listener.resolveStatus(new SocketException()));
    }

    @Test
    void shouldMapSqlTransientConnectionException() {
        assertEquals(CNX_ERROR, listener.resolveStatus(new SQLTransientConnectionException()));
    }

    @Test
    void shouldMapSqlNonTransientConnectionException() {
        assertEquals(CNX_REFUSED, listener.resolveStatus(new SQLNonTransientConnectionException()));
    }

    @Test
    void shouldMapSqlRecoverableException() {
        assertEquals(CNX_INTERRUPTED, listener.resolveStatus(new SQLRecoverableException()));
    }
    @Test
    void shouldReturnVendorErrorCode() {
        assertEquals(RMT_ERROR, listener.resolveStatus(new SQLException("Duplicate key", "23000", 1062)));
    }

    @Test
    void shouldReturnSqlStatePrefix() {
        assertEquals(RMT_ERROR, listener.resolveStatus(new SQLException("duplicate", "23505", 0)));
    }

    @Test
    void shouldReturnUnknownForInvalidSqlState() {
        assertEquals(RMT_ERROR, listener.resolveStatus(new SQLException("error", "ABCDE", 0)));
    }

    @Test
    void shouldReturnUnknownWhenSqlStateIsNull() {
        assertEquals(RMT_ERROR, listener.resolveStatus(new SQLException("error", null, 0)));
    }

    @Test
    void shouldReturnUnknownError() {
        assertEquals(RMT_ERROR, listener.resolveStatus(new IllegalArgumentException()));
    }
}