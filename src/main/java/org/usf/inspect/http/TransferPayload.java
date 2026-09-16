package org.usf.inspect.http;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
public interface TransferPayload {
	
	byte[] bytes();
	
	long size();
	
	@Getter
	public class StreamPayload { //input/output stream payload

		private final AtomicLong size = new AtomicLong();
		@Setter private Instant start;
		@Setter private Instant end;
		//bytes, exception
	}
	
}