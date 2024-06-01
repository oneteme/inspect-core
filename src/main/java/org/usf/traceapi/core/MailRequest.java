package org.usf.traceapi.core;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MailRequest extends SessionStage {
	
	private String protocol; //FTP, FTPS
	private String host;
	private int port;
	private String subject;
	private String contentType;
	private String[] from;
 	private String[] recipients; 
	private String[] replyTo;
	private int size;
	private List<MailRequestStage> actions;

}
