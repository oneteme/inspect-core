package org.usf.traceapi.core;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
public final class MailRequest extends SessionStage {
	
	private String protocol; //FTP, FTPS
	private String host;
	private int port;
	private List<MailRequestStage> actions;
	private List<Mail> mails;

}
