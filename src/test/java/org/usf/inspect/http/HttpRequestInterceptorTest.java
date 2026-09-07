package org.usf.inspect.http;

import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.usf.inspect.core.HttpAction.EXCHANGE;
import static org.usf.inspect.core.HttpAction.STREAM;
import static org.usf.inspect.core.DualEventTracer.CONN_ERROR;
import static org.usf.inspect.core.DualEventTracer.CONN_REFUSED;
import static org.usf.inspect.core.DualEventTracer.CONN_SSL_ERROR;
import static org.usf.inspect.core.DualEventTracer.CONN_UNKNOWN_HOST;
import static org.usf.inspect.core.DualEventTracer.SUCCESS;
import static org.usf.inspect.core.TestTraceHub.clearTraces;
import static org.usf.inspect.core.TestTraceHub.getTraces;
import static org.usf.inspect.core.TraceAssertions.assertExceptionTrace;
import static org.usf.inspect.core.TraceAssertions.assertRequestSignal;
import static org.usf.inspect.core.TraceAssertions.assertRequestStage;
import static org.usf.inspect.core.TraceAssertions.assertRequestUpdate;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.core.HttpRequestUpdate;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

class HttpRequestInterceptorTest {
	
	private static String HOST =  "localhost";
	
	private final MockWebServer server = new MockWebServer();
	
	private final RestTemplate template = new RestTemplateBuilder()
			.interceptors(new HttpRequestInterceptor())
			.setConnectTimeout(ofMillis(100))
			.setReadTimeout(ofMillis(50))
			.build();

    @BeforeEach
    void setUp() throws IOException {
        server.start();
        clearTraces();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }
    
    @Test
    void test_connection_invalid_scheme() {
        testConnectError(CONN_ERROR, "unknown-scheme", HOST, -1, "unknown-scheme://localhost/test", ResourceAccessException.class);
    }
    
    @Test
    void test_connection_unknown_host() {
    	testConnectError(CONN_UNKNOWN_HOST, "http", "myhost", -1, "http://myhost/", ResourceAccessException.class);
    }
    
    @Test
    void test_connection_bad_port() {
    	testConnectError(CONN_REFUSED, "http", HOST, 1234, "http://localhost:1234/", ResourceAccessException.class);
    }
    
    @Test //SocketTimeoutException
 	void test_connection_timeout() {
    	server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        testConnectError(CONN_ERROR, "http", server.getHostName(), server.getPort(), server.url("/").toString(), ResourceAccessException.class);
 	}
    
    @Test
    void test_connection_reset() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        testConnectError(CONN_ERROR, "http", server.getHostName(), server.getPort(), server.url("/").toString(), ResourceAccessException.class);
    }
    
    @Test
    void test_connection_lost() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
        testConnectError(CONN_ERROR, "http", server.getHostName(), server.getPort(), server.url("/").toString(), ResourceAccessException.class);
    }
    
    @Test
 	void test_connection_ssl() throws Exception {
    	var sslContext = SSLContext.getInstance("TLS");
    	sslContext.init(null, null, null); 
		try(var secureServer = new MockWebServer()) {
	        secureServer.useHttps(sslContext.getSocketFactory(), false);
			secureServer.start();
			testConnectError(CONN_SSL_ERROR, "https", secureServer.getHostName(), secureServer.getPort(), secureServer.url("/").toString(), ResourceAccessException.class);
		}
 	}

    void testConnectError(int status, String scheme, String host, int port, String url, Class<? extends Exception> type) {

        var start = now();
        assertThrows(type, ()-> template.getForEntity(url, String.class));
        var end = now();
        
        assertConnectionFailedTraces(status, scheme, host, port, start, end, getTraces());
    }
    
  	static void assertConnectionFailedTraces(int status, String scheme, String host, int port, Instant beforeStart, Instant afterEnd, List<EventTrace> traces){
  		assertEquals(4, traces.size());
  		var idx = 0;
  		var sgn = assertRequestSignal(scheme, host, port, null, beforeStart, HttpRequestSignal.class, traces.get(idx++));
  		var stg = assertRequestStage(EXCHANGE.name(), null, sgn.getId(), idx, null, sgn.getStart(), HttpRequestStage.class, traces.get(idx++));
  		assertExceptionTrace(sgn.getId(), stg.getOrder(), traces.get(idx++));
  		assertRequestUpdate(status, sgn.getId(), null, stg.getEnd(), afterEnd, HttpRequestUpdate.class, traces.get(idx));
  	}
    
    @Test
    void test_http_call() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("OK"));
        
        var start = now();
        assertDoesNotThrow(()-> template.getForEntity(server.url("/api").toString(), String.class));
        var end = now();
        
        assertRequestTraces(start, end, getTraces());
    }
    
	void assertRequestTraces(Instant beforeStart, Instant afterEnd, List<EventTrace> traces){
		assertEquals(4, traces.size());
		var idx = 0;
		var signal = assertRequestSignal("http", server.getHostName(), server.getPort(), null, beforeStart, HttpRequestSignal.class, traces.get(idx++));
		var strStg = assertRequestStage(EXCHANGE.name(), null, signal.getId(), idx, null, signal.getStart(), HttpRequestStage.class, traces.get(idx++));
		var endStg = assertRequestStage(STREAM.name(), null, signal.getId(), idx, null, strStg.getEnd(), HttpRequestStage.class, traces.get(idx++));		
		assertRequestUpdate(SUCCESS, signal.getId(), null, endStg.getEnd(), afterEnd, HttpRequestUpdate.class, traces.get(idx));
	}
}
