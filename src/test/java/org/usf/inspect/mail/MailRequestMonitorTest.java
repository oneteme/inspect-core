package org.usf.inspect.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.usf.inspect.core.RequestCommonStatus.*;
import static org.usf.inspect.mail.MailRequestListener.resolveStatus;

import java.net.SocketException;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;


class MailRequestMonitorTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    void should_extract_smtp_code_from_messaging_exception() {
        assertEquals(SERVER_ERROR, resolveStatus(new MessagingException("SMTP error 550 Mailbox unavailable")));
    }

    @Test
    void should_return_unknown_when_messaging_exception_has_no_smtp_code() {
        assertEquals(SERVER_ERROR, resolveStatus(new MessagingException("Connection failed")));
    }

    @Test
    void should_return_connection_unavailable_when_message_is_null() {
        assertEquals(SERVER_ERROR, resolveStatus(new MessagingException()));
    }

    @Test
    void should_return_connection_unavailable_for_socket_exception() {
        assertEquals(CONN_ERROR, resolveStatus(new SocketException("Connection reset")));
    }

    @Test
    void should_return_authentication_error() {
        assertEquals(CLIENT_UNAUTHORIZED, resolveStatus(new AuthenticationFailedException("bad credentials")));
    }

    @Test //TODO : what for ??
    void should_send_mail_with_fake_smtp_server() throws Exception {

        Properties props = new Properties();
        props.put("mail.smtp.host", "localhost");
        props.put("mail.smtp.port", String.valueOf(greenMail.getSmtp().getPort()));

        Session session = Session.getInstance(props);
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress("sender@test.com"));
        message.setRecipients(Message.RecipientType.TO, "receiver@test.com");
        message.setSubject("integration test");
        message.setText("hello");

        Transport.send(message);

        boolean received = greenMail.waitForIncomingEmail(5000, 1);

        var mails = greenMail.getReceivedMessages();
        assertTrue(received);
        assertEquals(1, mails.length);
    }
}