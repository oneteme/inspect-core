package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.createFtpRequest;
import static org.usf.inspect.ftp.FtpAction.CONNECTION;
import static org.usf.inspect.ftp.FtpAction.DISCONNECTION;

import java.time.Instant;

import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
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
		req.createStage(CONNECTION, start, end, thw).emit();
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
		req.createStage(DISCONNECTION, start, end, thw).emit();
		req.runSynchronized(()-> req.setEnd(end));
		return req;
	}
	
	<T> ExecutionHandler<T> stageHandler(FtpAction action, String... args) {
		return (s,e,o,t)-> req.createStage(action, s, e, t, args);
	}
}
