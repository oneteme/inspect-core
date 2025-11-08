package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.FtpAction.CONNECTION;
import static org.usf.inspect.core.FtpAction.DISCONNECTION;
import static org.usf.inspect.core.FtpAction.EXECUTE;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.createFtpRequest;

import java.time.Instant;

import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.FtpCommand;
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
	
	public void handleConnection(Instant start, Instant end, Void v, Throwable thrw) throws JSchException {
		req.createStage(CONNECTION, start, end, thrw, null).emit(); //before end if thrw
		req.setThreadName(threadName());
		req.setStart(start);
		req.setProtocol("sftp");
		var cs = sftp.getSession(); //throws JSchException
		if(nonNull(cs)) {
			req.setHost(cs.getHost());
			req.setPort(cs.getPort());
			req.setUser(cs.getUserName());
			req.setServerVersion(cs.getServerVersion());
			req.setClientVersion(cs.getClientVersion());
		}
		if(nonNull(thrw)) { //if connection error
			req.setEnd(end);
		}
		req.emit();
	}

	public void handleDisconnection(Instant start, Instant end, Void v, Throwable thw) {
		req.createStage(DISCONNECTION, start, end, thw, null).emit();
		req.runSynchronized(()-> req.setEnd(end));
	}
	
	<T> ExecutionHandler<T> executeStageHandler(FtpCommand cmd, String... args) {
		return (s,e,o,t)-> req.createStage(EXECUTE, s, e, t, cmd, args).emit();
	}
}
