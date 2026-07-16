package org.usf.inspect.dir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.usf.inspect.core.ErrorCode.CONNECTION_UNAVAILABLE;
import static org.usf.inspect.core.ErrorCode.TIMEOUT_OR_INTERRUPTION;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;

import java.util.Hashtable;

import javax.naming.CommunicationException;
import javax.naming.InterruptedNamingException;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.DirContext;

import org.junit.jupiter.api.Test;

class DirectoryRequestMonitorTest {

    private final DirectoryRequestMonitor monitor =
            new DirectoryRequestMonitor();

    @Test
    void should_return_connection_unavailable_when_service_is_unavailable() {

        int code = monitor.checkException(
                new ServiceUnavailableException("Server unavailable"));

        assertEquals(CONNECTION_UNAVAILABLE.getCode(), code);
    }

    @Test
    void should_return_timeout_when_communication_exception_occurs() {

        int code = monitor.checkException(
                new CommunicationException("Read timed out"));

        assertEquals(TIMEOUT_OR_INTERRUPTION.getCode(), code);
    }

    @Test
    void should_return_connection_unavailable_when_interrupted() {

        int code = monitor.checkException(
                new InterruptedNamingException("Interrupted"));

        assertEquals(CONNECTION_UNAVAILABLE.getCode(), code);
    }

    @Test
    void should_extract_ldap_error_code() {

        NamingException ex =
                new NamingException(
                        "LDAP: error code 49 - Invalid Credentials");

        int code = monitor.checkException(ex);

        assertEquals(49, code);
    }

    @Test
    void should_extract_another_ldap_error_code() {

        NamingException ex =
                new NamingException(
                        "LDAP: error code 32 - No Such Object");

        int code = monitor.checkException(ex);

        assertEquals(32, code);
    }

    @Test
    void should_return_unknown_error_when_ldap_message_contains_no_error_code() {

        NamingException ex =
                new NamingException("Some LDAP failure");

        int code = monitor.checkException(ex);

        assertEquals(UNKNOWN_ERROR.getCode(), code);
    }

    @Test
    void should_return_unknown_error_when_ldap_message_is_null() {

        NamingException ex = new NamingException();

        int code = monitor.checkException(ex);

        assertEquals(UNKNOWN_ERROR.getCode(), code);
    }

    @Test
    void should_return_unknown_error_for_unknown_exception() {

        int code = monitor.checkException(
                new IllegalArgumentException("test"));

        assertEquals(UNKNOWN_ERROR.getCode(), code);
    }



    @Test
    void should_return_null_when_environment_variable_is_missing() throws Exception {

        DirContext context = mock(DirContext.class);

        when(context.getEnvironment())
                .thenReturn(new Hashtable<>());

        String value =
                DirectoryRequestMonitor.getEnvironmentVariable(
                        context,
                        "missing.key",
                        Object::toString);

        assertNull(value);
    }

    @Test
    void should_return_null_when_environment_is_null() throws Exception {

        DirContext context = mock(DirContext.class);

        when(context.getEnvironment()).thenReturn(null);

        String value =
                DirectoryRequestMonitor.getEnvironmentVariable(
                        context,
                        "missing.key",
                        Object::toString);

        assertNull(value);
    }

}