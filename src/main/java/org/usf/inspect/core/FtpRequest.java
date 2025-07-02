package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.prettyURLFormat;

import lombok.Getter;
import lombok.Setter;
/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class FtpRequest extends AbstractRequest<FtpRequestStage> {

	private String protocol; //FTP, FTPS
	private String host;
	private int port;  // -1 otherwise
	private String serverVersion;
	private String clientVersion;
	//ftp-collector
	
	@Override
	public String prettyFormat() {
		return prettyURLFormat(getUser(), protocol, host, port, null);
	}

	@Override
	protected FtpRequestStage createStage() {
		return new FtpRequestStage();
	}
}
