package org.usf.inspect.core;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@ToString
public final class Mail {

	private String subject;
	private String contentType;
	private String[] from;
 	private String[] recipients; 
	private String[] replyTo;
	private int size; //in bytes, -1 otherwise
	
}
