package org.usf.inspect.mail;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import jakarta.mail.internet.ParseException;

class MailRequestListenerTest {
	
	MailRequestListener listener = new MailRequestListener();

	@Test
	void testSignal_noConnexion(){
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
	
	static Stream<Arguments> testSignalArgs() {
		return Stream.of(
				arguments((URLName)null),
				arguments(new URLName(null, null, 0, null, null, null)),
				arguments(new URLName("smtp", null, 0, null, null, null)),
				arguments(new URLName("smtp", "localhost", 0, null, null, null)),
				arguments(new URLName("smtp", "localhost", 25, null, null, null)),
				arguments(new URLName("smtp", "localhost", 25, null, "user", null)),
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

}
