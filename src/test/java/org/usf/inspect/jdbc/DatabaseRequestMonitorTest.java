package org.usf.inspect.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.RequestCommonStatus.CONN_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.CONN_INTERRUPTED;
import static org.usf.inspect.core.RequestCommonStatus.CONN_REFUSED;
import static org.usf.inspect.core.RequestCommonStatus.CONN_UNKNOWN_HOST;
import static org.usf.inspect.core.RequestCommonStatus.SERVER_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.SERVER_TIMEOUT;
import static org.usf.inspect.jdbc.DatabaseRequestMonitor.resolveStatus;

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

    @Test
    void shouldMapSqlTimeoutException() {
        assertEquals(SERVER_TIMEOUT, resolveStatus(new SQLTimeoutException()));
    }

    @Test
    void shouldMapSocketTimeoutException() {
        assertEquals(SERVER_TIMEOUT, resolveStatus(new SocketTimeoutException()));
    }

    @Test
    void shouldMapInterruptedException() {
        assertEquals(CONN_INTERRUPTED, resolveStatus(new InterruptedException()));
    }

    @Test
    void shouldMapEofException() {
        assertEquals(CONN_ERROR, resolveStatus(new EOFException()));
    }

    @Test
    void shouldMapUnknownHostException() {
        assertEquals(CONN_UNKNOWN_HOST, resolveStatus(new UnknownHostException()));
    }

    @Test
    void shouldMapSocketException() {
        assertEquals(CONN_ERROR, resolveStatus(new SocketException()));
    }

    @Test
    void shouldMapSqlTransientConnectionException() {
        assertEquals(CONN_ERROR, resolveStatus(new SQLTransientConnectionException()));
    }

    @Test
    void shouldMapSqlNonTransientConnectionException() {
        assertEquals(CONN_REFUSED, resolveStatus(new SQLNonTransientConnectionException()));
    }

    @Test
    void shouldMapSqlRecoverableException() {
        assertEquals(CONN_INTERRUPTED, resolveStatus(new SQLRecoverableException()));
    }
    @Test
    void shouldReturnVendorErrorCode() {
        assertEquals(SERVER_ERROR, resolveStatus(new SQLException("Duplicate key", "23000", 1062)));
    }

    @Test
    void shouldReturnSqlStatePrefix() {
        assertEquals(SERVER_ERROR, resolveStatus(new SQLException("duplicate", "23505", 0)));
    }

    @Test
    void shouldReturnUnknownForInvalidSqlState() {
        assertEquals(SERVER_ERROR, resolveStatus(new SQLException("error", "ABCDE", 0)));
    }

    @Test
    void shouldReturnUnknownWhenSqlStateIsNull() {
        assertEquals(SERVER_ERROR, resolveStatus(new SQLException("error", null, 0)));
    }

    @Test
    void shouldReturnUnknownError() {
        assertEquals(SERVER_ERROR, resolveStatus(new IllegalArgumentException()));
    }
}