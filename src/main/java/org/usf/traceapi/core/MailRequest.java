package org.usf.traceapi.core;

import static org.usf.traceapi.core.Helper.prettyFormat;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@JsonIgnoreProperties("exception")
public final class MailRequest extends SessionStage {
	
	private String host;
	private int port;
	private List<MailRequestStage> actions;
	private List<Mail> mails;
	//mail-collector
	
	@Override
	public ExceptionInfo getException() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setException(ExceptionInfo exception) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		return prettyFormat(getUser(), "SMTP", host, port, null);
	}
}
