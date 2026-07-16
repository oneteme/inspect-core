package org.usf.inspect.mail;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

class MailRequestMonitorIT {

    private GreenMail greenMail;

    @BeforeEach
    void startServer() {

        // Création d'un serveur SMTP local sur le port 3026
        greenMail = new GreenMail(
                new ServerSetup(
                        3026,
                        null,
                        ServerSetup.PROTOCOL_SMTP
                )
        );

        // Démarrage du serveur SMTP avant chaque test
        greenMail.start();
    }

    @AfterEach
    void stopServer() {

        if (greenMail != null) {
            greenMail.stop();
        }
    }

    /**
     * Vérifie que l'application détecte correctement une perte de connexion SMTP :
     * un premier envoi de mail est effectué avec succès, puis le serveur SMTP est arrêté
     * et le second envoi doit échouer en générant une MessagingException.
     */
    @Test
    void shouldDetectConnectionLostWhenServerStops() throws Exception {

        // Configuration de la connexion au serveur SMTP loca
        Properties props = new Properties();
        props.put("mail.smtp.host", "localhost");
        props.put("mail.smtp.port", "3026");

        // Création d'une session mail avec cette configuration
        Session session = Session.getInstance(props);

        // Premier mail
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@test.com"));
        message.setRecipients(
                jakarta.mail.Message.RecipientType.TO,
                "receiver@test.com");
        message.setSubject("First mail");
        message.setText("Hello");

        Transport.send(message);

        // Vérifie que le serveur a bien reçu le mail
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));


        // Arrêt du serveur SMTP pour simuler une perte de connexion
        greenMail.stop();

        // Deuxième mail
        MimeMessage second = new MimeMessage(session);
        second.setFrom(new InternetAddress("sender@test.com"));
        second.setRecipients(
                jakarta.mail.Message.RecipientType.TO,
                "receiver@test.com");
        second.setSubject("Second mail");
        second.setText("This should fail");

        // Vérifie qu'une erreur est bien générée car le serveur SMTP est arrêté
        MessagingException exception =
                assertThrows(
                        MessagingException.class,
                        () -> Transport.send(second)
                );

        // Affiche toute la chaîne des erreurs rencontrées
        Throwable cause = exception;
        while (cause != null) {
            cause = cause.getCause();
        }

        // Vérifie que l'exception existe bien
        assertNotNull(exception);
    }
}