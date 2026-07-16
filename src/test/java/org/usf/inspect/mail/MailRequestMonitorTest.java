package org.usf.inspect.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import org.usf.inspect.core.ErrorCode;


class MailRequestMonitorTest {


    private MailRequestMonitor monitor;


    @RegisterExtension
    static GreenMailExtension greenMail =
            new GreenMailExtension(ServerSetupTest.SMTP);


    @BeforeEach
    void setup() {
        monitor = new MailRequestMonitor();
    }



    @Test
    void should_extract_smtp_code_from_messaging_exception() {

        MessagingException exception =
                new MessagingException(
                        "SMTP error 550 Mailbox unavailable"
                );


        int code =
                monitor.checkException(exception);




        assertEquals(
                550,
                code
        );
    }



    @Test
    void should_return_unknown_when_messaging_exception_has_no_smtp_code() {

        MessagingException exception =
                new MessagingException(
                        "Connection failed"
                );




        int code =
                monitor.checkException(exception);

        assertEquals(
                ErrorCode.UNKNOWN_ERROR.getCode(),
                code
        );
    }



    @Test
    void should_return_connection_unavailable_when_message_is_null() {

        MessagingException exception =
                new MessagingException();


        int code =
                monitor.checkException(exception);


        assertEquals(
                ErrorCode.CONNECTION_UNAVAILABLE.getCode(),
                code
        );
    }



    @Test
    void should_return_connection_unavailable_for_socket_exception() {

        SocketException exception =
                new SocketException(
                        "Connection reset"
                );


        int code =
                monitor.checkException(exception);


        assertEquals(
                ErrorCode.CONNECTION_UNAVAILABLE.getCode(),
                code
        );
    }



    @Test
    void should_return_authentication_error() {

        AuthenticationFailedException exception =
                new AuthenticationFailedException(
                        "bad credentials"
                );


        int code =
                monitor.checkException(exception);


        assertEquals(
                ErrorCode.AUTHENTIFICATION_ERROR.getCode(),
                code
        );
    }



    @Test
    void should_send_mail_with_fake_smtp_server()
            throws Exception {


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

        boolean received =
                greenMail.waitForIncomingEmail(
                        5000,
                        1
                );

        var mails =
                greenMail.getReceivedMessages();


        assertTrue(received);

        assertEquals(
                1,
                mails.length
        );
    }
}