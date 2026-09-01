package org.usf.inspect.mail;

import static com.icegreen.greenmail.util.ServerSetup.SMTP;
import static jakarta.mail.Message.RecipientType.TO;
import static java.lang.System.getProperty;
import static java.time.Instant.now;
import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.usf.inspect.core.MailAction.CONNECTION;
import static org.usf.inspect.core.MailAction.DISCONNECTION;
import static org.usf.inspect.core.MailAction.EXECUTE;
import static org.usf.inspect.core.MailCommand.SEND;
import static org.usf.inspect.core.StatefulExecutionListener.CLIENT_UNAUTHORIZED;
import static org.usf.inspect.core.StatefulExecutionListener.CONN_ERROR;
import static org.usf.inspect.core.StatefulExecutionListener.CONN_REFUSED;
import static org.usf.inspect.core.StatefulExecutionListener.CONN_SSL_ERROR;
import static org.usf.inspect.core.StatefulExecutionListener.CONN_TIMEOUT;
import static org.usf.inspect.core.StatefulExecutionListener.CONN_UNKNOWN_HOST;
import static org.usf.inspect.core.StatefulExecutionListener.SERVER_ERROR;
import static org.usf.inspect.core.StatefulExecutionListener.SUCCESS;
import static org.usf.inspect.core.TraceAssertions.assertExceptionTrace;
import static org.usf.inspect.core.TraceAssertions.assertRequestSignal;
import static org.usf.inspect.core.TraceAssertions.assertRequestStage;
import static org.usf.inspect.core.TraceAssertions.assertRequestUpdate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.angus.mail.util.MailConnectException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.MailRequestSignal;
import org.usf.inspect.core.MailRequestStage;
import org.usf.inspect.core.MailRequestUpdate;
import org.usf.inspect.core.TestTraceHub;

import com.icegreen.greenmail.util.GreenMail;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * 
 * @author u$f
 *
 */
class TransportWrapperTest {

	private static String HOST = "localhost";  // greenMail.getSmtp().getBindTo() return 127.0.0.1
	private static String PORT = "25";
	
	private static GreenMail greenMail = new GreenMail(SMTP);
	private static String USER = getProperty("user.name");

	@BeforeEach
	void setUp() {
		greenMail.start();
	}

	@AfterEach
	void tearDown() {
		greenMail.stop();
	}
	
	@Test
	void test_connection_unknown_host() throws NoSuchProviderException {
		var props = initProperties("myhost", PORT, emptyMap());
		testConnectError(CONN_UNKNOWN_HOST, MailConnectException.class, props);
	}
	
	@Test
	void test_connection_bad_port() throws NoSuchProviderException {
		var props = initProperties(HOST, "125", emptyMap());
		testConnectError(CONN_REFUSED, MailConnectException.class, props);
	}
	
//	@Test unstable
	void test_connect_timeout() throws NoSuchProviderException {
		var props = initProperties(HOST, PORT, Map.of("mail.smtp.connectiontimeout", 1));
		testConnectError(CONN_TIMEOUT, MessagingException.class, props);
	}
	
	@Test
	void test_connection_tls_active() throws NoSuchProviderException {
		var props = initProperties(HOST, PORT, Map.of("mail.smtp.starttls.required", true));
		testConnectError(SERVER_ERROR, MessagingException.class, props);
	}

	@Test
	void test_connection_ssl_active() throws NoSuchProviderException {
		var props = initProperties(HOST, PORT, Map.of("mail.smtp.ssl.enable", true));
		testConnectError(CONN_SSL_ERROR, MessagingException.class, props);
	}
	
	@Test
	void test_connection_unauthenticate() throws NoSuchProviderException {
		var props = initProperties(HOST, PORT, Map.of("mail.smtp.auth", true));
		testConnectError(CLIENT_UNAUTHORIZED, AuthenticationFailedException.class, props);
	}
	
	void testConnectError(int status, Class<? extends Exception> type, Properties props) throws NoSuchProviderException{
        var ses = Session.getInstance(props); 
		var hub = new TestTraceHub();
		var wrp = new TransportWrapper(ses.getTransport(), new MailRequestListener(hub));

		var start = now();
		assertThrows(type, wrp::connect); 
		var end = now();
		
		assertConnectionFailedTraces(status, props, start, end, hub.getTraces());
	}
	
	static void assertConnectionFailedTraces(int status, Properties props, Instant beforeStart, Instant afterEnd, List<EventTrace> traces) {
		assertEquals(4, traces.size());
		int idx=0;

		var sgn = assertRequestSignal("smtp", props.getProperty("mail.smtp.host"), -1, USER, beforeStart, MailRequestSignal.class, traces.get(idx++));
		var stg = assertRequestStage(CONNECTION.name(), null, sgn.getId(), idx, null, sgn.getStart(), MailRequestStage.class, traces.get(idx++));
		assertExceptionTrace(sgn.getId(), idx-1, traces.get(idx++));
		assertRequestUpdate(status, sgn.getId(), null, stg.getEnd(), afterEnd, MailRequestUpdate.class, traces.get(idx));
	}
	
	@Test
	void test_connection_lost() throws MessagingException {
        var props = initProperties(HOST, PORT, emptyMap());

        var ses = Session.getInstance(props);
		var hub = new TestTraceHub();
		var wrp = new TransportWrapper(ses.getTransport(), new MailRequestListener(hub));

		var start = now();
		wrp.connect();
		try {
			greenMail.stop();
			var msg = buildMessage(ses);
			var rcp = msg.getAllRecipients();
			assertThrows(MessagingException.class, ()-> wrp.sendMessage(msg, rcp));
		}
		finally {
			wrp.close();
		}
		var end = now();
		assertConnectionLostTraces(CONN_ERROR, start, end, hub.getTraces());
	}
	
	static void assertConnectionLostTraces(int status, Instant beforeStart, Instant afterEnd, List<EventTrace> traces) {
		assertEquals(6, traces.size());
		var idx=0;

		var signal = assertRequestSignal("smtp", HOST, -1, USER, beforeStart, MailRequestSignal.class, traces.get(idx++));
		var cnxStg = assertRequestStage(CONNECTION.name(), null, signal.getId(), idx, null, signal.getStart(), MailRequestStage.class, traces.get(idx++));
		var sndStg = assertRequestStage(EXECUTE.name(), SEND.name(), signal.getId(), idx, null, cnxStg.getEnd(), MailRequestStage.class, traces.get(idx++));
		assertExceptionTrace(signal.getId(), idx-1, traces.get(idx++));
		var dscStg = assertRequestStage(DISCONNECTION.name(), null, signal.getId(), idx-1, null, sndStg.getEnd(), MailRequestStage.class, traces.get(idx++));
		assertRequestUpdate(status, signal.getId(), "EMIT", dscStg.getEnd(), afterEnd, MailRequestUpdate.class, traces.get(idx));
	}

	@ParameterizedTest
	@ValueSource(ints = {0,1,10})
	void test_send_message(int nMail) throws MessagingException {
		 var props = initProperties(HOST, PORT, emptyMap());
        var ses = Session.getInstance(props);
        var arr = new Message[nMail];
        for(var i=0; i<arr.length; i++) {
        	arr[i] = buildMessage(ses);
        }
		var before = now();
		var traces = assertDoesNotThrow(()-> sendMail(ses.getTransport(), arr));
		var after = now();

		assertRequestTraces(nMail, before, after, traces);
	}
	
	static void assertRequestTraces(int nMail, Instant beforeStart, Instant afterEnd, List<EventTrace> traces){
		assertEquals(4+nMail, traces.size());
		var idx = 0;
		
		var signal = assertRequestSignal("smtp", HOST, -1, USER, beforeStart, MailRequestSignal.class, traces.get(idx++));
		var prvStg = assertRequestStage(CONNECTION.name(), null, signal.getId(), idx, null, signal.getStart(), MailRequestStage.class, traces.get(idx++));
		for(int i=0; i<nMail; i++) {
			prvStg = assertRequestStage(EXECUTE.name(), SEND.name(), signal.getId(), idx, null, prvStg.getEnd(), MailRequestStage.class, traces.get(idx++));
		}
		assertRequestStage(DISCONNECTION.name(), null, signal.getId(), idx, null, prvStg.getEnd(), MailRequestStage.class, traces.get(idx++));
		var cmd = nMail > 0 ? "EMIT" : null;
		assertRequestUpdate(SUCCESS, signal.getId(), cmd, prvStg.getEnd(), afterEnd, MailRequestUpdate.class, traces.get(idx));
	}
	
	static Message buildMessage(Session session) throws  MessagingException {
        var msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("sender@test.com"));
        msg.setRecipients(TO, "receiver@test.com");
        msg.setSubject("First mail");
        msg.setText("Hello");
        return msg;
	}

	static List<EventTrace> sendMail(Transport trsp, Message... arr) throws MessagingException {
		var hub = new TestTraceHub();
		var wrp = new TransportWrapper(trsp, new MailRequestListener(hub));
		wrp.connect();
		try {
			if(nonNull(arr)) {
				for(var msg : arr) {
					wrp.sendMessage(msg, msg.getAllRecipients());
				}
			}
		}
		finally {
			wrp.close();
		}
		return hub.getTraces();
	}
	
	static Properties initProperties(String host, String port, Map<String, Object> map) {
        var props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        if(nonNull(map)) {
        	map.forEach(props::put);
        }
        return props;
	}
}
