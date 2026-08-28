package org.usf.inspect.mail;

import static com.icegreen.greenmail.util.ServerSetup.SMTP;
import static java.lang.Thread.currentThread;
import static java.time.Instant.now;
import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.angus.mail.util.MailConnectException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.core.InspectCollectorConfiguration;
import org.usf.inspect.core.MailRequestSignal;
import org.usf.inspect.core.MailRequestStage;
import org.usf.inspect.core.MailRequestUpdate;
import org.usf.inspect.core.TraceHub;

import com.icegreen.greenmail.util.GreenMail;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
class TransportWrapperTest {

	private static GreenMail greenMail = new GreenMail(SMTP);
	private static String HOST = "localhost";  // greenMail.getSmtp().getBindTo() return 127.0.0.1
	private static String PORT = "25";

	@BeforeEach
	void startServer() {
		greenMail.start();
	}

	@AfterEach
	void stopServer() {
		greenMail.stop();
	}
	
	@Test
	void test_connection_unknown_host() throws NoSuchProviderException {
		var props = initProperties("myhost", PORT, emptyMap());
		testConnectError(props, MailConnectException.class, CONN_UNKNOWN_HOST);
	}
	
	@Test
	void test_connection_bad_port() throws NoSuchProviderException {
		var props = initProperties(HOST, "125", emptyMap());
		testConnectError(props, MailConnectException.class, CONN_REFUSED);
	}
	
//	@Test unstable
	void test_connect_timeout() throws NoSuchProviderException {
		var props = initProperties(HOST, PORT, Map.of("mail.smtp.connectiontimeout", 1));
		testConnectError(props, MessagingException.class, CONN_TIMEOUT);
	}
	
	@Test
	void test_connection_tls_active() throws NoSuchProviderException {
		var props = initProperties(HOST, PORT, Map.of("mail.smtp.starttls.required", true));
		testConnectError(props, MessagingException.class, SERVER_ERROR);
	}

	@Test
	void test_connection_ssl_active() throws NoSuchProviderException {
		var props = initProperties(HOST, PORT, Map.of("mail.smtp.ssl.enable", true));
		testConnectError(props, MessagingException.class, CONN_SSL_ERROR);
	}
	
	@Test
	void test_connection_unauthenticate() throws NoSuchProviderException {
		var props = initProperties(HOST, PORT, Map.of("mail.smtp.auth", true));
		testConnectError(props, AuthenticationFailedException.class, CLIENT_UNAUTHORIZED);
	}
	
	void testConnectError(Properties props, Class<? extends Exception> type, int status) throws NoSuchProviderException{
        var ses = Session.getInstance(props);
		var hub = new TestTraceHub();
		var wrp = new TransportWrapper(ses.getTransport(), new MailRequestListener(hub));

		var start = now();
		assertThrows(type, wrp::connect); 
		var end = now();
		
		assertConnectionFailedTraces(status, props.getProperty("mail.smtp.host"), start, end, hub.getTraces());
	}
	
	static void assertConnectionFailedTraces(int status, String host, Instant beforeStart, Instant afterEnd, List<EventTrace> traces) {
		assertEquals(4, traces.size());
		int idx=0;

		var signal = assertInstanceOf(MailRequestSignal.class, traces.get(idx++));
		assertMailSignal(signal, beforeStart, host);

		var cnxStg = assertInstanceOf(MailRequestStage.class, traces.get(idx++));
		assertMailStage(cnxStg, signal.getStart(), CONNECTION.name(), null, signal.getId(), 1);

		var expTrc = assertInstanceOf(ExceptionTrace.class, traces.get(idx++));
		assertExceptionTrace(signal.getId(), 1, expTrc);
		
		var update = assertInstanceOf(MailRequestUpdate.class, traces.get(idx));
		assertMailUpdate(0, status, cnxStg.getEnd(), afterEnd, update);
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

		var signal = assertInstanceOf(MailRequestSignal.class, traces.get(idx++));
		assertMailSignal(signal, beforeStart, HOST);

		var cnxStg = assertInstanceOf(MailRequestStage.class, traces.get(idx++));
		assertMailStage(cnxStg, signal.getStart(), CONNECTION.name(), null, signal.getId(), 1);

		var sndStg = assertInstanceOf(MailRequestStage.class, traces.get(idx++));
		assertMailStage(sndStg, cnxStg.getEnd(), EXECUTE.name(), SEND.name(), signal.getId(), 2);

		var expTrc = assertInstanceOf(ExceptionTrace.class, traces.get(idx++));
		assertExceptionTrace(signal.getId(), 2, expTrc);
		
		var dscStg = assertInstanceOf(MailRequestStage.class, traces.get(idx++));
		assertMailStage(dscStg, sndStg.getEnd(), DISCONNECTION.name(), null, signal.getId(), 3);
		
		var update = assertInstanceOf(MailRequestUpdate.class, traces.get(idx));
		assertMailUpdate(1, status, dscStg.getEnd(), afterEnd, update);
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

		assertMailTraces(nMail, before, after, traces);
	}
	
	static void assertMailTraces(int nMail, Instant beforeStart, Instant afterEnd, List<EventTrace> traces){
		assertEquals(4+nMail, traces.size());
		var idx = 0;
		var signal = assertInstanceOf(MailRequestSignal.class, traces.get(idx++));
		assertMailSignal(signal, beforeStart, HOST);
		
		var strStg = assertInstanceOf(MailRequestStage.class, traces.get(idx++));
		assertMailStage(strStg, signal.getStart(), CONNECTION.name(), null, signal.getId(), idx-1);
		
		for(int i=0; i<nMail; i++) {
			var prv = (MailRequestStage) traces.get(idx-1);
			var sndStg = assertInstanceOf(MailRequestStage.class, traces.get(idx++));
			assertMailStage(sndStg, prv.getEnd(), EXECUTE.name(), SEND.name(), signal.getId(), idx-1);
		}
		var prv = (MailRequestStage) traces.get(idx-1);
		var endStg = assertInstanceOf(MailRequestStage.class, traces.get(idx++));
		assertMailStage(endStg, prv.getEnd(), DISCONNECTION.name(), null, signal.getId(), idx-1);
		
		var update = assertInstanceOf(MailRequestUpdate.class, traces.get(idx));
		assertMailUpdate(nMail, SUCCESS, endStg.getEnd(), afterEnd, update);
	}
	
	static void assertMailSignal(MailRequestSignal signal, Instant beforeStart, String host){
		assertNotNull(signal.getId());
		assertNull(signal.getSessionId()); //no active session
		assertEquals(currentThread().getName(), signal.getThreadName());
		assertTrue(signal.getStart().compareTo(beforeStart) >= 0);
		
		assertEquals("smtp", signal.getProtocol());
		assertEquals(host, signal.getHost());
//		assertEquals(3026, signal.getPort()); return -1
//		assertEquals(signal.getUser());
	}
	
	static void assertMailStage(MailRequestStage stage, Instant prvStageEnd, String action, String cmd, UUID id, int order){
		assertEquals(action, stage.getName());
		assertTrue(stage.getStart().compareTo(prvStageEnd) >= 0);
		assertTrue(stage.getEnd().compareTo(stage.getStart()) >= 0);
		assertEquals(cmd, stage.getCommand());
		assertEquals(stage.getRequestId(), id);
		assertEquals(order, stage.getOrder());
		assertNull(stage.getPayload());
	}
	
	static void assertMailUpdate(int nMail, int status, Instant lastStageEnd, Instant afterEnd, MailRequestUpdate update){
		assertNotNull(update.getId());
		assertEquals(nMail > 0 ? "EMIT" : null, update.getCommand());
		assertEquals(status, update.getStatus());
		assertTrue(update.getEnd().compareTo(lastStageEnd) >= 0);
		assertTrue(update.getEnd().compareTo(afterEnd) <= 0);
	}
	
	static void assertExceptionTrace(UUID id, int offset, ExceptionTrace ex) {
		assertEquals(ex.getTraceId(), id);
		assertEquals(offset, ex.getOffset());
		assertNotNull(ex.getType()); //class
		assertNotNull(ex.getMessage());
		assertNull(ex.getStackTraceRows());
		assertNull(ex.getCause());
	}
	
	static Message buildMessage(Session session) throws  MessagingException {
        var msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("sender@test.com"));
        msg.setRecipients(
                jakarta.mail.Message.RecipientType.TO,
                "receiver@test.com");
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
	
	static class TestTraceHub implements TraceHub {

		@Getter
		private final List<EventTrace> traces = new ArrayList<>();

		@Override
		public InspectCollectorConfiguration getConfiguration() {
			return null; //TODO check this
		}

		@Override
		public boolean emitTrace(EventTrace trace) {
			return traces.add(trace);
		}
	}
}
