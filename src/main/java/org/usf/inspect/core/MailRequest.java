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
public class MailRequest extends SessionStage<MailRequestStage> {

	private String protocol; //smtp(s), imap, pop3
	private String host;
	private int port;
	private List<MailRequestStage> actions;
	private List<Mail> mails;
	//mail-collector
	
	@Override
	public boolean append(MailRequestStage action) {
		return actions.add(action);
	}
	
	@Override
	public String prettyFormat() {
		return prettyURLFormat(getUser(), protocol, host, port, null);
	}
}
