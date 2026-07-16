package org.usf.inspect.jdbc;

import static org.junit.jupiter.api.Assertions.*;
import static org.usf.inspect.core.ErrorCode.*;

import java.io.EOFException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DatabaseRequestMonitorTest {

    private DatabaseRequestMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new DatabaseRequestMonitor(new ConnectionMetadataCache());
    }

    @Test
    @DisplayName("Should map SQLTimeoutException")
    void shouldMapSqlTimeoutException() {

        int code = monitor.checkException(new SQLTimeoutException());

        assertEquals(TIMEOUT_OR_INTERRUPTION.getCode(), code);
    }

    @Test
    void shouldMapSocketTimeoutException() {

        int code = monitor.checkException(new SocketTimeoutException());

        assertEquals(TIMEOUT_OR_INTERRUPTION.getCode(), code);
    }

    @Test
    void shouldMapInterruptedException() {

        int code = monitor.checkException(new InterruptedException());

        assertEquals(TIMEOUT_OR_INTERRUPTION.getCode(), code);
    }

    @Test
    void shouldMapEofException() {

        int code = monitor.checkException(new EOFException());

        assertEquals(CONNECTION_UNAVAILABLE.getCode(), code);
    }

    @Test
    void shouldMapUnknownHostException() {

        int code = monitor.checkException(new UnknownHostException());

        assertEquals(CONNECTION_UNAVAILABLE.getCode(), code);
    }

    @Test
    void shouldMapSocketException() {

        int code = monitor.checkException(new SocketException());

        assertEquals(CONNECTION_UNAVAILABLE.getCode(), code);
    }

    @Test
    void shouldMapSqlTransientConnectionException() {

        int code = monitor.checkException(
                new SQLTransientConnectionException());

        assertEquals(CONNECTION_UNAVAILABLE.getCode(), code);
    }

    @Test
    void shouldMapSqlNonTransientConnectionException() {

        int code = monitor.checkException(
                new SQLNonTransientConnectionException());

        assertEquals(CONNECTION_UNAVAILABLE.getCode(), code);
    }

    @Test
    void shouldMapSqlRecoverableException() {

        int code = monitor.checkException(
                new SQLRecoverableException());

        assertEquals(CONNECTION_UNAVAILABLE.getCode(), code);
    }
    @Test
    void shouldReturnVendorErrorCode() {

        SQLException ex =
                new SQLException(
                        "Duplicate key",
                        "23000",
                        1062);

        int code = monitor.checkException(ex);

        assertEquals(1062, code);
    }

    @Test
    void shouldReturnSqlStatePrefix() {

        SQLException ex =
                new SQLException(
                        "duplicate",
                        "23505",
                        0);

        int code = monitor.checkException(ex);

        assertEquals(23505, code);
    }

    @Test
    void shouldReturnUnknownForInvalidSqlState() {

        SQLException ex =
                new SQLException(
                        "error",
                        "ABCDE",
                        0);

        int code = monitor.checkException(ex);

        assertEquals(UNKNOWN_ERROR.getCode(), code);
    }

    @Test
    void shouldReturnUnknownWhenSqlStateIsNull() {

        SQLException ex =
                new SQLException(
                        "error",
                        null,
                        0);

        int code = monitor.checkException(ex);

        assertEquals(UNKNOWN_ERROR.getCode(), code);
    }

    @Test
    void shouldReturnUnknownError() {

        int code =
                monitor.checkException(
                        new IllegalArgumentException());

        assertEquals(UNKNOWN_ERROR.getCode(), code);
    }
}