package org.usf.inspect.ftp;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_CONNECTION_LOST;
import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_CONNECTION;
import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;
import static com.jcraft.jsch.ChannelSftp.SSH_FX_PERMISSION_DENIED;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;
import static org.usf.inspect.core.FtpAction.CONNECTION;
import static org.usf.inspect.core.FtpAction.DISCONNECTION;
import static org.usf.inspect.core.FtpAction.EXECUTE;
import static org.usf.inspect.core.SessionContextManager.createFtpSignal;

import java.time.Instant;

import org.usf.inspect.core.FtpAction;
import org.usf.inspect.core.FtpCommand;
import org.usf.inspect.core.FtpRequestSignal;
import org.usf.inspect.core.FtpRequestStage;
import org.usf.inspect.core.FtpRequestUpdate;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Monitor.StageBuilder;
import org.usf.inspect.core.StagePayload;
import org.usf.inspect.core.StatefulExecutionListener;
import org.usf.inspect.core.TraceSignal;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;

/**
 * 
 * @author u$f
 *
 */
final class FtpRequestListener extends StatefulExecutionListener<ChannelSftp> {
	
	@Override
	protected FtpRequestSignal signal(Instant start, ChannelSftp cnx) throws JSchException {
		var sgn = createFtpSignal(start);
		sgn.setProtocol("sftp");
		var cs = cnx.getSession(); //throws JSchException
		if(nonNull(cs)) {
			sgn.setHost(cs.getHost());
			sgn.setPort(cs.getPort());
			sgn.setUser(cs.getUserName());
			sgn.setServerVersion(cs.getServerVersion());
			sgn.setClientVersion(cs.getClientVersion());
		}
		return sgn;
	}

	@Override
	protected FtpRequestUpdate update(TraceSignal signal) { 
		return new FtpRequestUpdate(signal.getId());
	}

	@Override
	protected int resolveStatus(Throwable t) {
		return switch(t) {
			case com.jcraft.jsch.JSchException e -> CONN_REFUSED;
			case com.jcraft.jsch.SftpException e -> switch(e.id) { // funct.
				case SSH_FX_CONNECTION_LOST, SSH_FX_NO_CONNECTION -> CONN_ERROR;
				case SSH_FX_PERMISSION_DENIED -> CLIENT_UNAUTHORIZED;
				case SSH_FX_NO_SUCH_FILE -> CLIENT_ERROR;
			    default -> SERVER_ERROR;
			};
			default -> super.resolveStatus(t);
		};
	}

	ExecutionListener<Void> connectionListener(ChannelSftp sftp) {
		return connectionListener(stageBuilder(CONNECTION, null), v-> sftp); //before end if thrw
	}
	
	ExecutionListener<Void> disconnectionListener() {
		return disconnectionListener(stageBuilder(DISCONNECTION, null));
	}
	
	<T> ExecutionListener<T> executeStageListener(FtpCommand cmd, String... args) {
		return stageListener(EXECUTE, cmd, args);
	}

	<T> ExecutionListener<T> stageListener(FtpAction action, FtpCommand cmd, String... args) {
		if(nonNull(cmd) && nonNull(getTrace())) {
			var upd = (FtpRequestUpdate) getTrace();
			upd.setCommand(merge(upd.getCommand(), cmd.getType()));
		}
		return stageListener(stageBuilder(action, cmd, args));
	}
	
	<R> StageBuilder<R> stageBuilder(FtpAction action, FtpCommand cmd, String... args) {
		return (s,e,o,t)-> {
			var upd = getTrace();
			var stg = new FtpRequestStage(upd.getId(), getStageCounter().incrementAndGet());
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			if(nonNull(cmd)) {
				stg.setCommand(cmd.name());
			}
			stg.setPayload(nonNull(args) && args.length > 0 ? new StagePayload(args, null) : null);
			return stg;
		};
	}
}
