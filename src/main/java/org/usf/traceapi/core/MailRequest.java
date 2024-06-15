package org.usf.traceapi.core;

import static org.usf.traceapi.core.Helper.prettyURLFormat;

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
	
	private String host;
	private int port;
	private List<MailRequestStage> actions;
	private List<Mail> mails;
	//mail-collector
	
	@Override
	public String prettyFormat() {
		return prettyURLFormat(getUser(), "SMTP", host, port, null);
	}
}
