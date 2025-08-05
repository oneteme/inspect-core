package org.usf.inspect.core;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static org.usf.inspect.core.InspectContext.context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
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
		this.instance = instance; //register on next dispatch
	}

	@Override
	public void dispatch(boolean complete, int attempts, int pending, EventTrace[] traces)  {
		if(isInstanceRegistred()) {
			try {
				var uri = fromUriString(properties.getTracesURI())
						.queryParam("attempts", attempts)
						.queryParam("pending", pending)
						.queryParamIfPresent ("end", complete ? Optional.of(now()) : empty())
						.buildAndExpand(instance.getId()).toUri();
				template.put(uri, traces);
			}
			catch (RestClientException e) { //server / client ?
				throw new DispatchException("traces dispatch error", e);
			}
		}
	}

	@Override
	public void dispatch(int attempts, File dumpFile) {
		if(isInstanceRegistred()) {
			try {
				var uri = fromUriString(properties.getTracesURI())
						.queryParam("attempts", attempts)
						.queryParam("filename", dumpFile.getName())
						.buildAndExpand(instance.getId()).toUri();
				template.put(uri, mapper.readTree(dumpFile));
			}
			catch (RestClientException e) { //server / client ?
				throw new DispatchException("dump file dispatch error", e);
			}
			catch (IOException e) {
				throw new DispatchException("dump file read error " + dumpFile, e);
			}
		}
	}

	boolean isInstanceRegistred() {
		if(!registred && nonNull(instance)) { //dispatch traces before instance !
			try {
				template.postForObject(properties.getInstanceURI(), instance, String.class);
				log.info("instance was registred with id={}", instance.getId());
				registred = true;
			}
			catch(RestClientException e) {//server / client ?
				throw new DispatchException("instance register error", e);
			}
		}
		return registred;
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
					context().reportError("request body compression error", e);
				}
			}
			return exec.execute(req, body);
		};
	}
}
