package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.prettyURLFormat;

import java.time.Instant;

import org.usf.inspect.ftp.FtpAction;

import lombok.Getter;
import lombok.Setter;
/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class FtpRequest extends AbstractRequest {

	private String protocol; //FTP, FTPS
	private String host;
	private int port;  // -1 otherwise
	private String serverVersion;
	private String clientVersion;
	//v1.1
	private boolean failed;
	//ftp-collector
	
	public FtpRequestStage createStage(FtpAction type, Instant start, Instant end, Throwable t, String... args) {
		var stg = createStage(type, start, end, t, FtpRequestStage::new);
		stg.setArgs(args);
		return stg;
	}
	
	@Override
	public FtpRequest copy() {
		var req = new FtpRequest();
		req.setId(getId());
		req.setStart(getStart());
		req.setEnd(getEnd());
		req.setUser(getUser());
		req.setThreadName(getThreadName());
		req.setSessionId(getSessionId());
		req.setHost(host);
		req.setPort(port);
		req.setProtocol(protocol);
		req.setServerVersion(serverVersion);
		req.setClientVersion(clientVersion);
		req.setFailed(failed);
		return req;
	}
	
	@Override
	String prettyFormat() {
		return prettyURLFormat(getUser(), protocol, host, port, null);
	}
}
