package org.usf.inspect.ftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Set;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.FileHandle;
import org.apache.sshd.sftp.server.SftpFileSystemAccessor;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.apache.sshd.sftp.server.SftpSubsystemProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.usf.inspect.core.ConnectionLifecycleTracer;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;


class FtpRequestMonitorIT {

    private SshServer sshd;


    @BeforeEach
    void startServer() throws Exception {

        // création
        sshd = SshServer.setUpDefaultServer();

        // configuration
        sshd.setPort(0);

        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

        sshd.setPasswordAuthenticator(AcceptAllPasswordAuthenticator.INSTANCE);


        Path root =
                Files.createTempDirectory("mina-sftp");


        sshd.setFileSystemFactory(new VirtualFileSystemFactory(root));


        SftpSubsystemFactory sftp =
                new SftpSubsystemFactory.Builder()
                        .withFileSystemAccessor(
                                new SftpFileSystemAccessor() {

                                    @Override
                                    public SeekableByteChannel openFile(
                                            SftpSubsystemProxy subsystem,
                                            FileHandle fileHandle,
                                            Path file,
                                            String handle,
                                            Set<? extends OpenOption> options,
                                            FileAttribute<?>... attrs)
                                            throws IOException {

                                        throw new AccessDeniedException(
                                                file.toString()
                                        );
                                    }
                                }
                        )
                        .build();


        sshd.setSubsystemFactories(List.of(sftp));

        // démarrage
        sshd.start();
    }



    @AfterEach
    void stopServer() throws Exception {

        if (sshd != null && sshd.isStarted()) {
            sshd.stop(true);
        }
    }



    @Test
    void shouldReturnPermissionDenied() throws Exception {

        // Création du client SFTP
        JSch jsch = new JSch();

        // Connexion au serveur SFTP démarré dans le @BeforeEach
        Session session =
                jsch.getSession(
                        "user",
                        "localhost",
                        sshd.getPort()
                );


        session.setPassword("password");

        session.setConfig(
                "StrictHostKeyChecking",
                "no"
        );

        // Ouverture de la connexion SSH
        session.connect();

        // Ouverture du canal SFTP
        ChannelSftp sftp =
                (ChannelSftp) session.openChannel("sftp");


        sftp.connect();

        // Classe testée
        FtpConnectionLifecycleTracer monitor =
                new FtpConnectionLifecycleTracer();


    // Une tentative d'écriture doit être refusée par le serveur
        SftpException exception =
                assertThrows(
                        SftpException.class,
                        () -> sftp.put(
                                new ByteArrayInputStream(
                                        "hello".getBytes()
                                ),
                                "file.txt"
                        )
                );


        // Vérifie le code d'erreur renvoyé par le serveur
        assertEquals(
                ChannelSftp.SSH_FX_PERMISSION_DENIED,
                exception.id
        );

        // Vérifie le mapping effectué par FtpRequestMonitor
        assertEquals(
        		ConnectionLifecycleTracer.CLIENT_UNAUTHORIZED,
                monitor.resolveStatus(exception)
        );

        // Fermeture de la connexion
        sftp.disconnect();
        session.disconnect();
    }


    /**
     * Tests d'intégration du monitoring des requêtes FTP/SFTP.
     * Vérifie la gestion des erreurs de permission et des pertes de connexion.
     */

    @Test
    void shouldDetectConnectionLostWhenServerStops()
            throws Exception {

// Connexion au serveur
        JSch jsch = new JSch();


        Session session =
                jsch.getSession(
                        "user",
                        "localhost",
                        sshd.getPort()
                );


        session.setPassword("password");

        session.setConfig(
                "StrictHostKeyChecking",
                "no"
        );


        session.connect();



        ChannelSftp sftp =
                (ChannelSftp) session.openChannel("sftp");


        sftp.connect();



        // Première requête : le serveur répond encore
                assertThrows(
                        SftpException.class,
                        () -> sftp.put(
                                new ByteArrayInputStream(
                                        "hello".getBytes()
                                ),
                                "file.txt"
                        )
                );


        sshd.stop(true);

        // Laisse le temps au serveur de s'arrêter

        Thread.sleep(500);



        // Nouvelle requête après l'arrêt du serveur
        SftpException exception =
                assertThrows(
                        SftpException.class,
                        () -> sftp.put(
                                new ByteArrayInputStream(
                                        "again".getBytes()
                                ),
                                "file2.txt"
                        )
                );


        // Affiche la chaîne des causes pour le diagnostic
        Throwable cause = exception;


        while (cause != null) {


            cause = cause.getCause();
        }

        // Vérification du traitement de l'exception

        FtpConnectionLifecycleTracer monitor =
                new FtpConnectionLifecycleTracer();



        int code =
                monitor.resolveStatus(exception != null ? exception : null);


      // Une exception doit bien être remontée
        assertNotNull(exception);
        assertNotEquals(0, code);

        sftp.disconnect();
        session.disconnect();
    }
}