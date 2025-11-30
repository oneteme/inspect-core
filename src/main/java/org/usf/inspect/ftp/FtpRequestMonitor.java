package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Callback.assertStillOpened;
import static org.usf.inspect.core.FtpAction.CONNECTION;
import static org.usf.inspect.core.FtpAction.DISCONNECTION;
import static org.usf.inspect.core.FtpAction.EXECUTE;
import static org.usf.inspect.core.SessionContextManager.createFtpRequest;

import java.time.Instant;

import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
import org.usf.inspect.core.FtpCommand;
import org.usf.inspect.core.FtpRequestCallback;

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

	private final ChannelSftp sftp;
	private FtpRequestCallback callback;
	
	public void handleConnection(Instant start, Instant end, Void v, Throwable thrw) throws JSchException {
		var req = createFtpRequest(start);
		req.setProtocol("sftp");
		var cs = sftp.getSession(); //throws JSchException
		if(nonNull(cs)) {
			req.setHost(cs.getHost());
			req.setPort(cs.getPort());
			req.setUser(cs.getUserName());
			req.setServerVersion(cs.getServerVersion());
			req.setClientVersion(cs.getClientVersion());
		}
		req.emit();
		callback = req.createCallback();
		callback.createStage(CONNECTION, start, end, thrw, null).emit(); //before end if thrw
		if(nonNull(thrw)) { //if connection error
			callback.setEnd(end);
			callback.emit();
			callback = null;
		}
	}

	public void handleDisconnection(Instant start, Instant end, Void v, Throwable thw) {
		if(assertStillOpened(callback)) { //report if request was closed, avoid emit trace twice
			callback.createStage(DISCONNECTION, start, end, thw, null).emit();
			callback.setEnd(end);
			callback.emit();
			callback = null;
		}
	}
	
	<T> ExecutionHandler<T> executeStageHandler(FtpCommand cmd, String... args) {
		return (s,e,o,t)-> {
			if(assertStillOpened(callback)) {//report if request was closed
				callback.createStage(EXECUTE, s, e, t, cmd, args).emit();
			}
		};
	}
}
