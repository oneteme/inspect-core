package org.usf.traceapi.core;

import java.util.List;

import org.usf.traceapi.mail.Mail;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public final class MailRequest extends SessionStage {
	
	private String protocol; //FTP, FTPS
	private String host;
	private int port;
	private List<MailRequestStage> actions;
	private List<Mail> mails;

}
