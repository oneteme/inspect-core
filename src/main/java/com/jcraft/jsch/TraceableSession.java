package com.jcraft.jsch;

import static java.time.Instant.now;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;

import org.usf.traceapi.core.FtpRequest;

public class TraceableSession extends Session { //jsch session hack
	
	private final FtpRequest req; 

	public TraceableSession(JSch jsch, String username, String host, int port, FtpRequest req) throws JSchException {
		super(jsch, username, host, port);
		this.req = req;
	}
	
	@Override
	public void connect() throws JSchException {
		req.setStart(now());
		try {
			super.connect();
		}
		catch (Exception e) {
			req.setException(mainCauseException(e));
		}
	}
	
	@Override
	public void connect(int connectTimeout) throws JSchException {
		req.setStart(now());
		try {
			super.connect(connectTimeout);
		}
		catch (Exception e) {
			req.setException(mainCauseException(e));
		}
	}
	
	@Override
	public void disconnect() {
		try {
			super.disconnect();
		}
		catch (Exception e) {
			req.setException(mainCauseException(e));
		}
		finally {
			req.setEnd(now());
		}
	}
	
	
	public static void main(String[] args) {
		var 
		env = System.getProperties();
		for (var envName : env.keySet()) {
		    System.out.format("%s=%s%n", envName, env.get(envName));
		}
	}
}
