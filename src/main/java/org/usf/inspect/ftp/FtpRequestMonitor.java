package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.FtpAction.CONNECTION;
import static org.usf.inspect.core.FtpAction.DISCONNECTION;
import static org.usf.inspect.core.FtpAction.EXECUTE;
import static org.usf.inspect.core.Monitor.traceBegin;
import static org.usf.inspect.core.Monitor.traceStep;
import static org.usf.inspect.core.Monitor.traceEnd;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.FtpCommand;
import org.usf.inspect.core.FtpRequest2;
import org.usf.inspect.core.FtpRequestCallback;
import org.usf.inspect.core.Monitor;
import org.usf.inspect.core.SessionContextManager;

import com.jcraft.jsch.ChannelSftp;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
final class FtpRequestMonitor implements Monitor {

	private FtpRequestCallback callback;
	
	public ExecutionListener<Void>  handleConnection(ChannelSftp sftp) {
		return traceBegin(SessionContextManager::createFtpRequest, this::createCallback, (req,o)->{
			req.setProtocol("sftp");
			var cs = sftp.getSession(); //throws JSchException
			if(nonNull(cs)) {
				req.setHost(cs.getHost());
				req.setPort(cs.getPort());
				req.setUser(cs.getUserName());
				req.setServerVersion(cs.getServerVersion());
				req.setClientVersion(cs.getClientVersion());
			}
		}, (s,e,o,t)-> callback.createStage(CONNECTION, s, e, t, null)); //before end if thrw
	}
	
	//callback should be created before processing
	FtpRequestCallback createCallback(FtpRequest2 session) { 
		return callback = session.createCallback();
	}
	
	public ExecutionListener<Void> handleDisconnection() {
		return traceEnd(callback, (s,e,o,t)-> callback.createStage(DISCONNECTION, s, e, t, null));
	}
	
	<T> ExecutionListener<T> executeStageHandler(FtpCommand cmd, String... args) {
		return traceStep(callback, (s,e,o,t)-> callback.createStage(EXECUTE, s, e, t, cmd, args));
	}
}
