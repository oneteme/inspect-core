package org.usf.inspect.core;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class RestDispatcherAgent implements DispatcherAgent {
	
	private final RestRemoteServerProperties properties;
	private final ObjectMapper mapper;
	private final RestTemplate template;

	private InstanceEnvironment instance;
	private boolean registred = false;

	public RestDispatcherAgent(RestRemoteServerProperties properties, ObjectMapper mapper) {
		this(properties, mapper, defaultRestTemplate(properties, mapper));
	}
	
	@Override
	public void dispatch(InstanceEnvironment instance) {
		this.instance = instance;
	}
	
	@Override
    public void dispatch(boolean complete, int attemps, int pending, List<EventTrace> traces)  {
		if(!registred && nonNull(instance)) { //dispatch traces before instance !
			try {
				template.postForObject(properties.getInstanceURI(), instance, String.class);
				log.info("instance was registred with id={}", instance.getId());
				registred = true;
			}
			catch(RestClientException e) {
				throw new DispatchException("instance register error", e);
			}
		}
    	if(registred) {
    		try {
    			template.put(properties.getTracesURI(), traces, instance.getId(), attemps, pending, complete ? now() : null);
    		}
    		catch (RestClientException e) {
				throw new DispatchException("traces dispatch error", e);
			}
    	}
    }
	
	@Override
	public void dispatch(int attempts, File dumpFile) {
		try {
			template.put(properties.getTracesURI(), mapper.readTree(dumpFile), instance.getId(), attempts, null, null);
		}
		catch (RestClientException e) {
			throw new DispatchException("dump file dispatch error", e);
		}
		catch (IOException e) {
			throw new DispatchException("cannot read dump file " + dumpFile, e);
		}
	}
	
	static RestTemplate defaultRestTemplate(RestRemoteServerProperties properties, ObjectMapper mapper) {
		var json = new MappingJackson2HttpMessageConverter(mapper);
		var plain = new StringHttpMessageConverter(); //for instanceID
	    var timeout = ofSeconds(600); //wait for server startup 
	    var rt = new RestTemplateBuilder()
	    		.messageConverters(json, plain) //minimum converters
				.setConnectTimeout(timeout)
				.setReadTimeout(timeout)
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
	    if(properties.getCompressMinSize() > 0) {
	    	rt = rt.interceptors(bodyCompressionInterceptor(properties));
	    }
	    return rt.build();
	}
	
	static ClientHttpRequestInterceptor bodyCompressionInterceptor(final RestRemoteServerProperties properties) {
		return (req, body, exec)->{
			if(body.length >= properties.getCompressMinSize()) {
			    var baos = new ByteArrayOutputStream();
			    try (var gos = new GZIPOutputStream(baos)) {
			        gos.write(body);
				    req.getHeaders().add(CONTENT_ENCODING, "gzip");
			        body = baos.toByteArray();
			    }
			    catch (Exception e) {/*do not throw exception */
			    	log.warn("cannot compress sessions, {}", e.getMessage());
			    }
			}
	    	return exec.execute(req, body);
		};
	}
}
