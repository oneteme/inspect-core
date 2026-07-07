package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorCode.*;
import static org.usf.inspect.core.FtpAction.CONNECTION;
import static org.usf.inspect.core.FtpAction.DISCONNECTION;
import static org.usf.inspect.core.FtpAction.EXECUTE;

import org.usf.inspect.core.FtpAction;
import org.usf.inspect.core.FtpCommand;
import org.usf.inspect.core.FtpRequestSignal;
import org.usf.inspect.core.FtpRequestUpdate;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Monitor.StatefulMonitor;
import org.usf.inspect.core.SessionContextManager;

import com.jcraft.jsch.ChannelSftp;

/**
 * 
 * @author u$f
 *
 */
final class FtpRequestMonitor extends StatefulMonitor<FtpRequestSignal, FtpRequestUpdate> {

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
		}, stageHandler(CONNECTION, null)); //before end if thrw
	}
	
	//callback should be created before processing
	protected FtpRequestUpdate createCallback(FtpRequestSignal session) { 
		return session.createCallback();
	}
	
	ExecutionListener<Object> disconnectionHandler() {
		return traceEnd(stageHandler(DISCONNECTION, null));
	}
	
	<T> ExecutionListener<T> executeStageHandler(FtpCommand cmd, String... args) {
		return stageHandler(EXECUTE, cmd, args);
	}

	<T> ExecutionListener<T> stageHandler(FtpAction action, FtpCommand cmd, String... args) {
		return traceStep((s,e,o,t)-> getCallback().createStage(action, s, e, t, cmd, this::checkException, args));
	}



	public int checkException(Throwable t) {
		return switch(t) {

			case java.net.UnknownHostException e->
					CONNECTION_UNAVAILABLE.getCode();

			//timeout
			case java.net.SocketTimeoutException e->
					TIMEOUT_OR_INTERRUPTION.getCode();

			case java.net.SocketException e ->
					CONNECTION_UNAVAILABLE.getCode();

			case com.jcraft.jsch.JSchException e ->
					CONNECTION_UNAVAILABLE.getCode();

			// SFTP métier
			case com.jcraft.jsch.SftpException e ->
					e.id;

			default -> UNKNOWN_ERROR.getCode();
		};
	}
}
