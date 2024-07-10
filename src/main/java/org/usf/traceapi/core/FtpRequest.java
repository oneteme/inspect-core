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
@Getter
@Setter
public final class FtpRequest extends SessionStage {

	private String protocol; //FTP, FTPS
	private String host;
	private int port;  // -1 otherwise
	private String serverVersion;
	private String clientVersion;
	private List<FtpRequestStage> actions;
	//ftp-collector
	
	@Override
	public String prettyFormat() {
		return prettyURLFormat(getUser(), protocol, host, port, null);
	}
}
