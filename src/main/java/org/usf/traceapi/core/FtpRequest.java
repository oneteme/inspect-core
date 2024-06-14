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
@Getter
@Setter
@JsonIgnoreProperties("exception")
public final class FtpRequest extends SessionStage {

	private String protocol; //FTP, FTPS
	private String host;
	private int port;  // -1 otherwise
	private String serverVersion;
	private String clientVersion;
	private List<FtpRequestStage> actions;
	//ftp-collector
	
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
		return prettyFormat(getUser(), protocol, host, port, null);
	}
}
