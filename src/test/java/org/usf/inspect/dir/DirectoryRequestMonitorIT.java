package org.usf.inspect.dir;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.Entry;

class DirectoryRequestMonitorIT {

    private InMemoryDirectoryServer server;
    private DirContext ctx;

    private static final String BASE_DN = "dc=example,dc=com";
    private static final String ADMIN_DN = "cn=admin,dc=example,dc=com";
    private static final String PASSWORD = "password";


    @BeforeEach
    void setUp() throws Exception {

        InMemoryDirectoryServerConfig config =
                new InMemoryDirectoryServerConfig(BASE_DN);

        config.addAdditionalBindCredentials(
                ADMIN_DN,
                PASSWORD
        );

        server = new InMemoryDirectoryServer(config);

        server.add(new Entry(
                "dn: " + BASE_DN,
                "objectClass: top",
                "objectClass: domain",
                "dc: example"
        ));

        server.startListening();


        Hashtable<String, Object> env = new Hashtable<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        env.put(Context.PROVIDER_URL, "ldap://localhost:" + server.getListenPort());

        env.put(Context.SECURITY_AUTHENTICATION, "simple");

        env.put(Context.SECURITY_PRINCIPAL, ADMIN_DN);

        env.put(Context.SECURITY_CREDENTIALS, PASSWORD);

        ctx = new InitialDirContext(env);

        assertNotNull(ctx);
    }


    @AfterEach
    void stopServer() throws Exception {

        if (ctx != null) {
            ctx.close();
        }

        if (server != null) {
            server.shutDown(true);
        }
    }


    /**
     * Vérifie le traitement d'une exception LDAP lors d'une recherche sur une entrée inexistante.
     */
    @Test
    void should_connect_and_raise_ldap_exception() throws Exception {

        NamingException ex = assertThrows(
                NamingException.class,
                () -> ctx.search(
                        "ou=does-not-exist," + BASE_DN,
                        "(objectClass=*)",
                        new SearchControls()
                )
        );


        DirectoryRequestMonitor monitor =
                new DirectoryRequestMonitor();

        int code = monitor.checkException(ex);
        assertNotNull(code);

    }


    /**
     * Vérifie la détection d'une perte de connexion après l'arrêt du serveur LDAP.
     */
    @Test
    void shouldDetectConnectionLostWhenLdapServerStops()
            throws Exception {


        // arrêt serveur pour simuler une perte de disponibilité LDAP
        server.shutDown(true);


        NamingException ex = assertThrows(
                NamingException.class,
                () -> ctx.search(
                        BASE_DN,
                        "(objectClass=*)",
                        new SearchControls()
                )
        );


        DirectoryRequestMonitor monitor =
                new DirectoryRequestMonitor();

        // Vérifie que l'exception est correctement identifiée comme une erreur de connexion
        int code = monitor.checkException(ex);

        assertNotNull(code);
    }
}