package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.FtpAction.CONNECTION;
import static org.usf.inspect.core.FtpAction.DISCONNECTION;
import static org.usf.inspect.core.FtpAction.EXECUTE;

import org.usf.inspect.core.FtpAction;
import org.usf.inspect.core.FtpCommand;
import org.usf.inspect.core.FtpRequest2;
import org.usf.inspect.core.FtpRequestCallback;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Monitor.StatefulMonitor;
import org.usf.inspect.core.SessionContextManager;

import com.jcraft.jsch.ChannelSftp;

/**
 * 
 * @author u$f
 *
 */
final class FtpRequestMonitor extends StatefulMonitor<FtpRequest2, FtpRequestCallback> {

	ExecutionListener<Object> connectionHandler(ChannelSftp sftp) {
		return traceBegin(SessionContextManager::createFtpRequest, (req,o)->{
			req.setProtocol("sftp");
			var cs = sftp.getSession(); //throws JSchException
			if(nonNull(cs)) {
				req.setHost(cs.getHost());
				req.setPort(cs.getPort());
				req.setUser(cs.getUserName());
				req.setServerVersion(cs.getServerVersion());
				req.setClientVersion(cs.getClientVersion());
			}
		}).then(stageHandler(CONNECTION, null)); //before end if thrw
	}
	
	//callback should be created before processing
	protected FtpRequestCallback createCallback(FtpRequest2 session) { 
		return session.createCallback();
	}
	
	ExecutionListener<Object> disconnectionHandler() {
		return stageHandler(DISCONNECTION, null).then(traceEnd());
	}
	
	<T> ExecutionListener<T> executeStageHandler(FtpCommand cmd, String... args) {
		return stageHandler(EXECUTE, cmd, args);
	}

	<T> ExecutionListener<T> stageHandler(FtpAction action, FtpCommand cmd, String... args) {
		return traceStep((s,e,o,t)-> callback.createStage(action, s, e, t, cmd, args));
	}
}
