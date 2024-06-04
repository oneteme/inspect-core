package org.usf.traceapi.core;

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
	private int port;
	private String serverVersion;
	private String clientVersion;
	private List<FtpRequestStage> actions;
	//fta-collector
}
