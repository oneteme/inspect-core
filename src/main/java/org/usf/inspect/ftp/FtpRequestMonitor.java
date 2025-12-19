package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.FtpAction.CONNECTION;
import static org.usf.inspect.core.FtpAction.DISCONNECTION;
import static org.usf.inspect.core.FtpAction.EXECUTE;
import static org.usf.inspect.core.Monitor.traceBegin;
import static org.usf.inspect.core.Monitor.traceEnd;
import static org.usf.inspect.core.Monitor.traceStep;

import org.usf.inspect.core.FtpAction;
import org.usf.inspect.core.FtpCommand;
import org.usf.inspect.core.FtpRequest2;
import org.usf.inspect.core.FtpRequestCallback;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
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
	
	ExecutionListener<Void> connectionHandler(ChannelSftp sftp) {
		ExecutionListener<Void> lstn = traceBegin(SessionContextManager::createFtpRequest, this::createCallback, (req,o)->{
			req.setProtocol("sftp");
			var cs = sftp.getSession(); //throws JSchException
			if(nonNull(cs)) {
				req.setHost(cs.getHost());
				req.setPort(cs.getPort());
				req.setUser(cs.getUserName());
				req.setServerVersion(cs.getServerVersion());
				req.setClientVersion(cs.getClientVersion());
			}
		});
		return lstn.then(stageHandler(CONNECTION, null)); //before end if thrw
	}
	
	//callback should be created before processing
	FtpRequestCallback createCallback(FtpRequest2 session) { 
		return callback = session.createCallback();
	}
	
	ExecutionListener<Void> disconnectionHandler() {
		ExecutionListener<Void> lstn = stageHandler(DISCONNECTION, null);
		return lstn.then(traceEnd(callback));
	}
	
	<T> ExecutionListener<T> executeStageHandler(FtpCommand cmd, String... args) {
		return stageHandler(EXECUTE, cmd, args);
	}

	<T> ExecutionListener<T> stageHandler(FtpAction action, FtpCommand cmd, String... args) {
		return traceStep(callback, (s,e,o,t)-> callback.createStage(action, s, e, t, cmd, args));
	}
}
