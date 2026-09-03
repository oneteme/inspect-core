package org.usf.inspect.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.Monitor2.CONN_ERROR;
import static org.usf.inspect.core.Monitor2.CONN_INTERRUPTED;
import static org.usf.inspect.core.Monitor2.CONN_REFUSED;
import static org.usf.inspect.core.Monitor2.CONN_UNKNOWN_HOST;
import static org.usf.inspect.core.Monitor2.SERVER_ERROR;
import static org.usf.inspect.core.Monitor2.SERVER_TIMEOUT;

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

	private final DatabaseRequestListener listener = new DatabaseRequestListener(null);

    @Test
    void shouldMapSqlTimeoutException() {
        assertEquals(SERVER_TIMEOUT, listener.resolveStatus(new SQLTimeoutException()));
    }

    @Test
    void shouldMapSocketTimeoutException() {
        assertEquals(SERVER_TIMEOUT, listener.resolveStatus(new SocketTimeoutException()));
    }

    @Test
    void shouldMapInterruptedException() {
        assertEquals(CONN_INTERRUPTED, listener.resolveStatus(new InterruptedException()));
    }

    @Test
    void shouldMapEofException() {
        assertEquals(CONN_ERROR, listener.resolveStatus(new EOFException()));
    }

    @Test
    void shouldMapUnknownHostException() {
        assertEquals(CONN_UNKNOWN_HOST, listener.resolveStatus(new UnknownHostException()));
    }

    @Test
    void shouldMapSocketException() {
        assertEquals(CONN_ERROR, listener.resolveStatus(new SocketException()));
    }

    @Test
    void shouldMapSqlTransientConnectionException() {
        assertEquals(CONN_ERROR, listener.resolveStatus(new SQLTransientConnectionException()));
    }

    @Test
    void shouldMapSqlNonTransientConnectionException() {
        assertEquals(CONN_REFUSED, listener.resolveStatus(new SQLNonTransientConnectionException()));
    }

    @Test
    void shouldMapSqlRecoverableException() {
        assertEquals(CONN_INTERRUPTED, listener.resolveStatus(new SQLRecoverableException()));
    }
    @Test
    void shouldReturnVendorErrorCode() {
        assertEquals(SERVER_ERROR, listener.resolveStatus(new SQLException("Duplicate key", "23000", 1062)));
    }

    @Test
    void shouldReturnSqlStatePrefix() {
        assertEquals(SERVER_ERROR, listener.resolveStatus(new SQLException("duplicate", "23505", 0)));
    }

    @Test
    void shouldReturnUnknownForInvalidSqlState() {
        assertEquals(SERVER_ERROR, listener.resolveStatus(new SQLException("error", "ABCDE", 0)));
    }

    @Test
    void shouldReturnUnknownWhenSqlStateIsNull() {
        assertEquals(SERVER_ERROR, listener.resolveStatus(new SQLException("error", null, 0)));
    }

    @Test
    void shouldReturnUnknownError() {
        assertEquals(SERVER_ERROR, listener.resolveStatus(new IllegalArgumentException()));
    }
}