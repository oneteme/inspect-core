package org.usf.inspect.jdbc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.h2.tools.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseConnectionFailureIT {

    private Server server;

    @BeforeEach
    void startServer() throws SQLException {

        server = Server.createTcpServer(
                "-tcp",
                "-tcpPort",
                "9123",
                "-ifNotExists"
        ).start();
    }

    @AfterEach
    void stopServer() {

        if (server != null) {
            server.stop();
        }
    }

    @Test
    void shouldDetectConnectionLostWhenServerStops() throws Exception {

        Connection cnx = DriverManager.getConnection(
                "jdbc:h2:tcp://localhost:9123/./target/testdb",
                "sa",
                ""
        );

        cnx.createStatement()
                .execute("DROP ALL OBJECTS");

        Statement st = cnx.createStatement();

        st.execute("""
                CREATE TABLE person(
                    id INT PRIMARY KEY
                )
                """);

        st.execute("""
                INSERT INTO person(id)
                VALUES(1)
                """);


        // Arrêt du serveur
        server.stop();

        SQLException exception =
                assertThrows(
                        SQLException.class,
                        () -> st.execute("""
                                SELECT * FROM person
                                """)
                );


        Throwable cause = exception;
        while (cause != null) {
            cause = cause.getCause();
        }

        DatabaseConnectionLifecycleTracer monitor =
                new DatabaseConnectionLifecycleTracer(
                        new ConnectionMetadataCache()
                );

        int code = monitor.resolveStatus(exception != null ? exception : null);

        assertNotNull(exception);
        assertNotEquals(0, code);

        cnx.close();
    }

    @Test
    void shouldDetectConnectionLostAfterIdleTime() throws Exception {

        Connection cnx = DriverManager.getConnection(
                "jdbc:h2:tcp://localhost:9123/./target/testdb",
                "sa",
                "");

        Statement st = cnx.createStatement();

        // Première requête : la connexion fonctionne
        st.executeQuery("SELECT 1");


        // Connexion inactive pendant quelques secondes
        Thread.sleep(3000);

        // Simulation de la perte de connexion
        server.stop();

        SQLException exception =
                assertThrows(
                        SQLException.class,
                        () -> st.executeQuery("SELECT 1")
                );

        DatabaseConnectionLifecycleTracer monitor =
                new DatabaseConnectionLifecycleTracer(
                        new ConnectionMetadataCache());

        int code = monitor.resolveStatus(exception);

        assertNotNull(exception);
        assertNotEquals(0, code);

        cnx.close();
    }


@Test
void shouldDetectConnectionLostWhenServerStopsDuringQuery() throws Exception {

    Connection cnx = DriverManager.getConnection(
            "jdbc:h2:tcp://localhost:9123/./target/testdb",
            "sa",
            ""
    );

    Statement st = cnx.createStatement();

    CountDownLatch started = new CountDownLatch(1);
    AtomicReference<SQLException> exceptionRef = new AtomicReference<>();

    Thread queryThread = new Thread(() -> {

        started.countDown();

        try {
            st.executeQuery("SELECT SLEEP(30000)");
        } catch (SQLException e) {
            exceptionRef.set(e);
        }

    });

    queryThread.start();

    // attendre que le thread ait démarré
    started.await();

    // laisser le temps à executeQuery() d'être envoyé au serveur
    Thread.sleep(500);

    server.stop();

    queryThread.join(5000);

    SQLException exception = exceptionRef.get();


    assertNotNull(exception, "La requête aurait dû échouer.");

    DatabaseConnectionLifecycleTracer monitor =
            new DatabaseConnectionLifecycleTracer(new ConnectionMetadataCache());

    int code = monitor.resolveStatus(exception);
    assertNotEquals(0, code);


    try {
        cnx.close();
    } catch (SQLException ignored) {
        // normal : la connexion est déjà cassée
    }
}


}