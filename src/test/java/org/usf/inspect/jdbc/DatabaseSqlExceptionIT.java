package org.usf.inspect.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.tools.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;



class DatabaseSqlExceptionIT {


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
    void shouldDetectDuplicateConstraintError() throws Exception {

        try (Connection cnx =
                     DriverManager.getConnection(
                             "jdbc:h2:tcp://localhost:9123/./target/test1;MODE=MySQL",
                             "sa",
                             "")) {

            cnx.createStatement()
                    .execute("DROP ALL OBJECTS");


            Statement st = cnx.createStatement();

            st.execute("""
                CREATE TABLE person(
                    id INTEGER PRIMARY KEY
                )
            """);

            st.execute("""
                INSERT INTO person(id)
                VALUES(1)
            """);

            SQLException exception =
                    assertThrows(
                            SQLException.class,
                            () -> st.execute("""
                                INSERT INTO person(id)
                                VALUES(1)
                            """)
                    );

            DatabaseRequestMonitor monitor =
                    new DatabaseRequestMonitor(
                            new ConnectionMetadataCache()
                    );

            int code = monitor.checkException(exception);


            assertEquals(23505, code);
        }
    }


    @Test
    void shouldDetectUnknownTableError() throws Exception {

        try (Connection cnx =
                     DriverManager.getConnection(
                             "jdbc:h2:mem:test2",
                             "sa",
                             "")) {

            Statement st = cnx.createStatement();

            SQLException exception =
                    assertThrows(
                            SQLException.class,
                            () -> st.execute("""
                                SELECT * FROM UNKNOWN_TABLE
                            """)
                    );

            DatabaseRequestMonitor monitor =
                    new DatabaseRequestMonitor(
                            new ConnectionMetadataCache()
                    );

            int code = monitor.checkException(exception);
            assertNotEquals(0, code);


        }
    }
}