package org.usf.inspect.ftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.core.DualEventTracer.CNX_ERROR;
import static org.usf.inspect.core.DualEventTracer.CNX_REFUSED;
import static org.usf.inspect.core.DualEventTracer.CNX_TIMEOUT;
import static org.usf.inspect.core.DualEventTracer.CNX_UNKNOWN_HOST;
import static org.usf.inspect.core.DualEventTracer.INT_ERROR;
import static org.usf.inspect.core.DualEventTracer.INT_TIMEOUT;

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
        assertEquals(CNX_UNKNOWN_HOST, listener.resolveStatus(new UnknownHostException("unknown host")));
    }

    @Test
    void should_return_timeout_for_socket_timeout() {
        assertEquals(INT_TIMEOUT, listener.resolveStatus(new SocketTimeoutException("read timeout ...")));
    }
    
    @Test
    void should_return_timeout_for_socket_timeout2() {
        assertEquals(CNX_TIMEOUT, listener.resolveStatus(new SocketTimeoutException("connect timeout ...")));
    }

    @Test
    void should_return_connection_unavailable_for_socket_exception() {
        assertEquals(CNX_ERROR, listener.resolveStatus(new SocketException("socket error")));
    }

    @Test
    void should_return_connection_unavailable_for_jsch_exception() {
        assertEquals(CNX_REFUSED, listener.resolveStatus(new JSchException("connection failed")));
    }

    @Test
    void should_return_sftp_exception_id()  {
        assertEquals(INT_ERROR, listener.resolveStatus(new SftpException(4, "Failure"))); //TODO resolver vendor code
    }

    @Test
    void should_return_unknown_error_for_unexpected_exception() {
        assertEquals(INT_ERROR, listener.resolveStatus(new RuntimeException("unexpected")));
    }
}