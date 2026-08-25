package org.usf.inspect.ftp;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_CONNECTION_LOST;
import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_CONNECTION;
import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;
import static com.jcraft.jsch.ChannelSftp.SSH_FX_PERMISSION_DENIED;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.FtpAction.CONNECTION;
import static org.usf.inspect.core.FtpAction.DISCONNECTION;
import static org.usf.inspect.core.FtpAction.EXECUTE;
import static org.usf.inspect.core.Helper.rootCauseException;
import static org.usf.inspect.core.RequestCommonStatus.CLIENT_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.CLIENT_UNAUTHORIZED;
import static org.usf.inspect.core.RequestCommonStatus.CONN_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.CONN_REFUSED;
import static org.usf.inspect.core.RequestCommonStatus.SERVER_ERROR;
import static org.usf.inspect.core.RequestCommonStatus.SUCCESS;
import static org.usf.inspect.core.RequestCommonStatus.statusFor;

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
		return traceStep((s,e,o,t)-> {
			var upd = getCallback();
			var stg = upd.createStage();
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			if(nonNull(cmd)) {
				stg.setCommand(cmd.name());
				upd.setCommand(merge(upd.getCommand(), cmd.getType()));
			}
			if(nonNull(t)) {
				var root = rootCauseException(t);
				stg.setException(fromException(root, 0, 0)); //no stack trace
				if(upd.getStatus() < 0 ||  upd.getStatus() == SUCCESS) { //if success or no error, set status
					upd.setStatus(resolveStatus(root));
				}
			}
			else {
				upd.setStatus(SUCCESS);
			}
			stg.setArgs(args);
			return stg;
		});
	}

	static int resolveStatus(Throwable t) {
		if (isNull(t)) {
	        return SUCCESS;
	    }
		return switch(t) {
			case com.jcraft.jsch.JSchException e -> CONN_REFUSED;
			case com.jcraft.jsch.SftpException e -> switch(e.id) { // funct.
				case SSH_FX_CONNECTION_LOST, SSH_FX_NO_CONNECTION -> CONN_ERROR;
				case SSH_FX_PERMISSION_DENIED -> CLIENT_UNAUTHORIZED;
				case SSH_FX_NO_SUCH_FILE -> CLIENT_ERROR;
			    default -> SERVER_ERROR;
			};
			default -> statusFor(t);
		};
	}
}
