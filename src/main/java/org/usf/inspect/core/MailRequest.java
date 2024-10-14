package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.prettyURLFormat;

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
public class MailRequest extends SessionStage {
	
	private String host;
	private int port;
	private List<MailRequestStage> actions;
	private List<Mail> mails;
	//mail-collector
	
	@Override
	public String prettyFormat() {
		return prettyURLFormat(getUser(), "smtp", host, port, null);
	}
}
