package org.usf.traceapi.core;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class FtpRequest extends RunnableStage {
	
	private String host;
	private int port;
	private String serverVersion;
	private String clientVersion;
	
	private List<FtpAction> actions = new LinkedList<>();
	//collector

}
