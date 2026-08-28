package org.usf.inspect.mail;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.usf.inspect.mail.MailRequestListener.mailTrace;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.ParseException;

class MailRequestListenerTest {
	
	MailRequestListener listener = new MailRequestListener();

	@Test
	void testSignal_null(){
		var start = now();
		var sgn = assertDoesNotThrow(()-> listener.signal(start, null));
		assertEquals(start, sgn.getStart());
		assertAllNotNull(sgn::getId, sgn::getThreadName);
		assertNull(sgn.getSessionId()); //current session is null
		assertAllNull(sgn::getProtocol, sgn::getHost, sgn::getUser);
		assertEquals(0, sgn.getPort());
	}
	
	@ParameterizedTest
	@MethodSource("testSignalArgs")
	void testSignal(URLName url){
		var cnx = mock(Transport.class);
		when(cnx.getURLName()).thenReturn(url);
		var start = now();
		var sgn = assertDoesNotThrow(()-> listener.signal(start, cnx));
		assertEquals(start, sgn.getStart());
		assertAllNotNull(sgn::getId, sgn::getThreadName);
		assertNull(sgn.getSessionId()); //current session is null
		if(nonNull(url)) {
			assertEquals(url.getProtocol(), sgn.getProtocol());
			assertEquals(url.getHost(), sgn.getHost());
			assertEquals(url.getUsername(), sgn.getUser());
			assertEquals(url.getPort(), sgn.getPort());
		}
	}

	@ParameterizedTest
	@MethodSource("testResolveStatusArgs")
	void testResolveStatus(Exception ex, int status){
		assertEquals(status, listener.resolveStatus(ex));
	}
	
	@Test
	void testUpdate(){
		var sgn = listener.signal(now(), null);
		assertEquals(sgn.getId(), listener.update(sgn).getId());
	}
	
	@Test
	void testMailTrace() throws MessagingException {
		assertNull(assertDoesNotThrow(()-> mailTrace(null)));
	}

	@ParameterizedTest
	@MethodSource("testMailTraceArgs")
	void testMailTrace(String subject, String cntType, String[] from, String[] rcp, String[] rpl, int size) throws MessagingException {
		var msg = mock(Message.class);
		when(msg.getSubject()).thenReturn(subject);
		when(msg.getContentType()).thenReturn(cntType);
		when(msg.getFrom()).thenReturn(toAddressArray(from));
		when(msg.getAllRecipients()).thenReturn(toAddressArray(rcp));
		when(msg.getReplyTo()).thenReturn(toAddressArray(rpl));
		when(msg.getSize()).thenReturn(size);
		
		var trc = assertDoesNotThrow(()-> mailTrace(msg));
		assertEquals(subject, trc.getSubject());
		assertEquals(cntType, trc.getContentType());
		assertArrayEquals(from, trc.getFrom());
		assertArrayEquals(rcp, trc.getRecipients());
		assertArrayEquals(rpl, trc.getReplyTo());
		assertEquals(size, trc.getSize());
	}
	
	static Stream<Arguments> testSignalArgs() {
		return Stream.of(
				arguments((URLName)null),
				arguments(new URLName(null, null, -1, null, null, null)),
				arguments(new URLName("smtp", null, 0, null, null, null)),
				arguments(new URLName("smtp", "localhost", 0, null, null, null)),
				arguments(new URLName("smtp", "localhost", 25, null, null, null)),
				arguments(new URLName("smtp", "localhost", 55, null, "user", null)),
				arguments(new URLName("smtp", "localhost", 25, null, "user", "pass")));
	}
	
	static Stream<Arguments> testResolveStatusArgs() {
		return Stream.of(
				arguments(new AuthenticationFailedException(), 401),
				arguments(new ParseException(), 400),
				arguments(new MessagingException(), 500),
				arguments(new IllegalArgumentException(), 500),
				arguments(new Exception(), 500));
	}
	
	static Stream<Arguments> testMailTraceArgs() {
		return Stream.of(
				arguments(null, null, null, null, null, 0),
				arguments("", "", new String[] {}, new String[] {}, new String[] {}, 1),
				arguments("subject", "text/plain", new String[] {"from"}, new String[] {"rcp"}, new String[] {"rpl"}, 10),
				arguments("subject", "text/html", new String[] {"from1", "from2"}, new String[] {"rcp1", "rcp2"}, new String[] {"rpl1", "rpl2"}, 20),
				arguments("subject", "text/html", new String[] {"from1", "from2"}, new String[] {"rcp1", "rcp2"}, null, 20),
				arguments("subject", "text/html", new String[] {"from1", "from2"}, null, new String[] {"rpl1", "rpl2"}, 20));
	}
	
	static void assertAllNull(Supplier<?>... fields) {
		if(nonNull(fields)) {
			for(var supp : fields) {
				assertNull(supp.get());
			}
		}
	}
	
	static void assertAllNotNull(Supplier<?>... fields) {
		if(nonNull(fields)) {
			for(var supp : fields) {
				assertNotNull(supp.get());
			}
		}
	}
	
	static Address[] toAddressArray(String... address) {
		if(nonNull(address)) {
			var arr = new Address[address.length];
			try {
				for(int i=0; i<address.length; i++) {
					arr[i] = new InternetAddress(address[i]);
				}
			} catch (AddressException e) {
				throw new IllegalArgumentException(e);
			}
			return arr;
		}
		return null;
	}
}
