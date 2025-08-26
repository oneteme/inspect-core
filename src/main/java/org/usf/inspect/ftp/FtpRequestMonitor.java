package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionManager.createFtpRequest;
import static org.usf.inspect.ftp.FtpAction.CONNECTION;
import static org.usf.inspect.ftp.FtpAction.DISCONNECTION;

import java.time.Instant;

import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.FtpRequest;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class FtpRequestMonitor {

	private final FtpRequest req = createFtpRequest();
	private final ChannelSftp sftp;
	
	public FtpRequest handleConnection(Instant start, Instant end, Void v, Throwable thw) throws JSchException {
		context().emitTrace(req.createStage(CONNECTION, start, end, thw));
		req.setThreadName(threadName());
		req.setStart(start);
		if(nonNull(thw)) { //if connection error
			req.setEnd(end);
		}
		req.setProtocol("sftp");
		var cs = sftp.getSession(); //throws JSchException
		if(nonNull(cs)) {
			req.setHost(cs.getHost());
			req.setPort(cs.getPort());
			req.setUser(cs.getUserName());
			req.setServerVersion(cs.getServerVersion());
			req.setClientVersion(cs.getClientVersion());
		}
		return req;
	}

	public FtpRequest handleDisconnection(Instant start, Instant end, Void v, Throwable thw) {
		context().emitTrace(req.createStage(DISCONNECTION, start, end, thw));
		req.runSynchronized(()-> req.setEnd(end));
		return req;
	}
	
	<T> ExecutionMonitorListener<T> stageHandler(FtpAction action, String... args) {
		return (s,e,o,t)-> req.createStage(action, s, e, t, args);
	}
}
