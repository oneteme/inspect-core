package org.usf.traceapi.mail;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public final class Mail {

	private String subject;
	private String contentType;
	private String[] from;
 	private String[] recipients; 
	private String[] replyTo;
	private int size;
	
}
