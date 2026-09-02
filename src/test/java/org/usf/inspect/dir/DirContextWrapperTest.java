package org.usf.inspect.dir;

import static com.unboundid.ldap.listener.InMemoryListenerConfig.createLDAPConfig;
import static java.time.Instant.now;
import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static javax.naming.Context.PROVIDER_URL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.usf.inspect.core.DirAction.CONNECTION;
import static org.usf.inspect.core.DirAction.DISCONNECTION;
import static org.usf.inspect.core.DirAction.EXECUTE;
import static org.usf.inspect.core.DirCommand.LIST;
import static org.usf.inspect.core.StatefulExecutionListener.CONN_REFUSED;
import static org.usf.inspect.core.StatefulExecutionListener.CONN_UNKNOWN_HOST;
import static org.usf.inspect.core.StatefulExecutionListener.SUCCESS;
import static org.usf.inspect.core.TestTraceHub.clearTraces;
import static org.usf.inspect.core.TestTraceHub.getTraces;
import static org.usf.inspect.core.TraceAssertions.assertExceptionTrace;
import static org.usf.inspect.core.TraceAssertions.assertRequestSignal;
import static org.usf.inspect.core.TraceAssertions.assertRequestStage;
import static org.usf.inspect.core.TraceAssertions.assertRequestUpdate;

import java.time.Instant;
import java.util.Hashtable;
import java.util.List;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.usf.inspect.core.DirectoryRequestSignal;
import org.usf.inspect.core.DirectoryRequestStage;
import org.usf.inspect.core.DirectoryRequestUpdate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.StagePayload;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;

/**
 * 
 * @author u$f
 *
 */
class DirContextWrapperTest {
	
	private static final String HOST = "localhost";
	
	private final InMemoryDirectoryServer server = setup();
	private final DirectoryRequestListener listener = new DirectoryRequestListener();
	
	
    @BeforeEach
    void setUp() throws Exception {
    	clearTraces();
        server.startListening();
    }

    @AfterEach
    void tearDown() {
        server.shutDown(true);
    }

    @Test
	void test_connection_unknown_host() {
		testConnectError(CONN_UNKNOWN_HOST, CommunicationException.class, "myhost", server.getListenPort());
	}
    
	@Test
	void test_connection_bad_port() {
		testConnectError(CONN_REFUSED, CommunicationException.class, HOST, 12345);
	}
    
	void testConnectError(int status, Class<? extends Exception> type, String host, int port) {
		var start = now();
		assertThrows(type, ()-> createClient(host, port));
        var end = now();
        
        assertConnectionFailedTraces(status, null, null, 0, start, end, getTraces());
	}
	
	static void assertConnectionFailedTraces(int status, String scheme, String host, int port, Instant beforeStart, Instant afterEnd, List<EventTrace> traces) {
		assertEquals(4, traces.size());
		int idx=0;

		var sgn = assertRequestSignal(scheme, host, port, null, beforeStart, DirectoryRequestSignal.class, traces.get(idx++));
		var stg = assertRequestStage(CONNECTION.name(), null, sgn.getId(), idx, null, sgn.getStart(), DirectoryRequestStage.class, traces.get(idx++));
		assertExceptionTrace(sgn.getId(), idx-1, traces.get(idx++));
		assertRequestUpdate(status, sgn.getId(), null, stg.getEnd(), afterEnd, DirectoryRequestUpdate.class, traces.get(idx));
	}
    
    @Test
//    @RepeatedTest(value = 100)
    void test_request_success() throws NamingException {
    	var start = now();
        var dir = assertDoesNotThrow(()-> createClient("localhost", server.getListenPort()));
        try {
        	assertDoesNotThrow(()-> dir.list("dc=jarvis,dc=usf"));
        }
        finally {
			dir.close();
		}
        var end = now();
        
        assertRequestTraces(server.getListenPort(), start, end, new StagePayload(new String[] {"dc=jarvis,dc=usf"}, null), getTraces());
        
//       System.err.println(TraceAssertions.performance(hub.getTraces()));
    }
    
	static void assertRequestTraces(int port, Instant beforeStart, Instant afterEnd, StagePayload sp, List<EventTrace> traces){
		assertEquals(5, traces.size());
		var idx = 0;
		var signal = assertRequestSignal("ldap", HOST, port, null, beforeStart, DirectoryRequestSignal.class, traces.get(idx++));
		var cnxStg = assertRequestStage(CONNECTION.name(), null, signal.getId(), idx, null, signal.getStart(), DirectoryRequestStage.class, traces.get(idx++));
		var excStg = assertRequestStage(EXECUTE.name(), LIST.name(), signal.getId(), idx, sp, cnxStg.getEnd(), DirectoryRequestStage.class, traces.get(idx++));
		var dscStg = assertRequestStage(DISCONNECTION.name(), null, signal.getId(), idx, null, excStg.getEnd(), DirectoryRequestStage.class, traces.get(idx++));
		assertRequestUpdate(SUCCESS, signal.getId(), "READ", dscStg.getEnd(), afterEnd, DirectoryRequestUpdate.class, traces.get(idx));
	}
    
    DirContextWrapper createClient(String host, int port) throws NamingException {
        var env = new Hashtable<String, Object>();
        env.put(INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(PROVIDER_URL, "ldap://%s:%d".formatted(host, port));
//        env.put(SECURITY_AUTHENTICATION, "simple");
//        env.put(Context.SECURITY_PRINCIPAL, ADMIN_DN);
//        env.put(Context.SECURITY_CREDENTIALS, PASSWORD);
        return new DirContextWrapper(listener, ()-> new InitialDirContext(env));
    }
    
    static InMemoryDirectoryServer setup() {
    	try {
            var config = new InMemoryDirectoryServerConfig("dc=jarvis,dc=usf");
            config.setListenerConfigs(createLDAPConfig("default", 0)); 
            
            var server = new InMemoryDirectoryServer(config);

        	server.add("dn: dc=jarvis,dc=usf", "objectClass: top", "objectClass: domain", "dc: jarvis");
            server.add("dn: uid=u$f,dc=jarvis,dc=usf", 
                       "objectClass: inetOrgPerson", 
                       "uid: u$f", 
                       "cn: Usf User", 
                       "sn: Usf", 
                       "userPassword: secretpassword");
            return server;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
    }
}
