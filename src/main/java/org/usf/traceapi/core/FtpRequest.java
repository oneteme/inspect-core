package org.usf.traceapi.core;

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
}
