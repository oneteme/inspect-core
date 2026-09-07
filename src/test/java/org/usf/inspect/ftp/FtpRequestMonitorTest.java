package org.usf.inspect.ftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.DualEventTracer.CONN_ERROR;
import static org.usf.inspect.core.DualEventTracer.CONN_REFUSED;
import static org.usf.inspect.core.DualEventTracer.CONN_TIMEOUT;
import static org.usf.inspect.core.DualEventTracer.CONN_UNKNOWN_HOST;
import static org.usf.inspect.core.DualEventTracer.SERVER_ERROR;
import static org.usf.inspect.core.DualEventTracer.SERVER_TIMEOUT;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

class FtpRequestMonitorTest {
	
	private final FtpConnectionLifecycleTracer listener = new FtpConnectionLifecycleTracer();

    @Test
    void should_return_connection_unavailable_for_unknown_host() {
        assertEquals(CONN_UNKNOWN_HOST, listener.resolveStatus(new UnknownHostException("unknown host")));
    }

    @Test
    void should_return_timeout_for_socket_timeout() {
        assertEquals(SERVER_TIMEOUT, listener.resolveStatus(new SocketTimeoutException("read timeout ...")));
    }
    
    @Test
    void should_return_timeout_for_socket_timeout2() {
        assertEquals(CONN_TIMEOUT, listener.resolveStatus(new SocketTimeoutException("connect timeout ...")));
    }

    @Test
    void should_return_connection_unavailable_for_socket_exception() {
        assertEquals(CONN_ERROR, listener.resolveStatus(new SocketException("socket error")));
    }

    @Test
    void should_return_connection_unavailable_for_jsch_exception() {
        assertEquals(CONN_REFUSED, listener.resolveStatus(new JSchException("connection failed")));
    }

    @Test
    void should_return_sftp_exception_id()  {
        assertEquals(SERVER_ERROR, listener.resolveStatus(new SftpException(4, "Failure"))); //TODO resolver vendor code
    }

    @Test
    void should_return_unknown_error_for_unexpected_exception() {
        assertEquals(SERVER_ERROR, listener.resolveStatus(new RuntimeException("unexpected")));
    }
}