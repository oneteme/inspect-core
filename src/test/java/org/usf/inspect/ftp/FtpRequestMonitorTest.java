package org.usf.inspect.ftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.ErrorCode.CONNECTION_UNAVAILABLE;
import static org.usf.inspect.core.ErrorCode.TIMEOUT_OR_INTERRUPTION;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.junit.jupiter.api.Test;

class FtpRequestMonitorTest {

    private final FtpRequestMonitor monitor = new FtpRequestMonitor();

    @Test
    void should_return_connection_unavailable_for_unknown_host() {

        int code = monitor.checkException(
                new UnknownHostException("unknown host")
        );

        assertEquals(CONNECTION_UNAVAILABLE.getCode(), code);
    }

    @Test
    void should_return_timeout_for_socket_timeout() {

        int code = monitor.checkException(
                new SocketTimeoutException("timeout")
        );

        assertEquals(TIMEOUT_OR_INTERRUPTION.getCode(), code);
    }

    @Test
    void should_return_connection_unavailable_for_socket_exception() {

        int code = monitor.checkException(
                new SocketException("socket error")
        );

        assertEquals(CONNECTION_UNAVAILABLE.getCode(), code);
    }

    @Test
    void should_return_connection_unavailable_for_jsch_exception() {

        int code = monitor.checkException(
                new JSchException("connection failed")
        );

        assertEquals(CONNECTION_UNAVAILABLE.getCode(), code);
    }

    @Test
    void should_return_sftp_exception_id()  {

        SftpException exception =
                new SftpException(4, "Failure");

        int code = monitor.checkException(exception);

        assertEquals(4, code);
    }

    @Test
    void should_return_unknown_error_for_unexpected_exception() {

        int code = monitor.checkException(
                new RuntimeException("unexpected")
        );

        assertEquals(UNKNOWN_ERROR.getCode(), code);
    }
}