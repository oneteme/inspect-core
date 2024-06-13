package org.usf.traceapi.core;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@JsonIgnoreProperties("exception")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

	public static MailRequest newMailRequest() {
		var req = new MailRequest();
		req.setActions(new ArrayList<>());
		req.setMails(new ArrayList<>());
		return req;
	}
}
