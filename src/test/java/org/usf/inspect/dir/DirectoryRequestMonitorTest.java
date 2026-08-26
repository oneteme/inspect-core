package org.usf.inspect.dir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.usf.inspect.core.RequestCommonStatus.CONN_INTERRUPTED;
import static org.usf.inspect.core.RequestCommonStatus.CONN_REFUSED;
import static org.usf.inspect.core.RequestCommonStatus.SERVER_ERROR;
import static org.usf.inspect.dir.DirectoryRequestListener.getEnvironmentVariable;
import static org.usf.inspect.dir.DirectoryRequestListener.resolveStatus;

import java.util.Hashtable;

import javax.naming.CommunicationException;
import javax.naming.InterruptedNamingException;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.DirContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DirectoryRequestMonitorTest {

    @Test
    void should_return_connection_unavailable_when_service_is_unavailable() {
        assertEquals(CONN_REFUSED, resolveStatus(new ServiceUnavailableException("Server unavailable")));
    }

    @Test
    void should_return_timeout_when_communication_exception_occurs() {
        assertEquals(CONN_INTERRUPTED, resolveStatus(new CommunicationException("Read timed out")));
    }

    @Test
    void should_return_connection_unavailable_when_interrupted() {
        assertEquals(CONN_INTERRUPTED, resolveStatus(new InterruptedNamingException("Interrupted")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
		"LDAP: error code 49 - Invalid Credentials",
        "LDAP: error code 32 - No Such Object",
        "Some LDAP failure",
    })
    void should_extract_ldap_error_code() {
        assertEquals(SERVER_ERROR, resolveStatus(new NamingException())); //TODO resolve vendor code
    }

    @Test
    void should_return_unknown_error_for_unknown_exception() {
        int ex = resolveStatus(new IllegalArgumentException("test"));
        assertEquals(SERVER_ERROR, ex);
    }

    @Test
    void should_return_null_when_environment_variable_is_missing() throws Exception {
        DirContext context = mock(DirContext.class);
        when(context.getEnvironment()).thenReturn(new Hashtable<>());
        assertNull(getEnvironmentVariable(context, "missing.key", Object::toString));
    }

    @Test
    void should_return_null_when_environment_is_null() throws Exception {
        DirContext context = mock(DirContext.class);
        when(context.getEnvironment()).thenReturn(null);
        assertNull(getEnvironmentVariable(context, "missing.key", Object::toString));
    }

}