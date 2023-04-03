package org.usf.traceapi.core;

import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class RemoteTraceSender implements TraceSender {

	private final String url;
	private final RestTemplate template;
	
	public RemoteTraceSender(String url) {
		this.url = url;
		this.template = new RestTemplate();
	}

	@Override
	public void send(MainRequest mr) {
		try {
			template.put(url, mr);
		}
		catch(Exception e) {
			log.warn("error while tracing request : {}", mr.getUrl(), e);
		}
	}
	
}
