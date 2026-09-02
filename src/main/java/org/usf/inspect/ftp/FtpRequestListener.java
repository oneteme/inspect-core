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

/**
 * 
 * @author u$f
 *
 */
final class FtpRequestListener extends StatefulExecutionListener {
	
	@Override
	public FtpRequestSignal signal(Instant start) {
		return createFtpSignal(start);
	}

	@Override
	public FtpRequestUpdate update(TraceSignal signal) { 
		return new FtpRequestUpdate(signal.getId());
	}

	@Override
	public int resolveStatus(Throwable t) {
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

	public ExecutionListener<Void> connectionListener(ChannelSftp sftp) {
		return connectionListener(stageBuilder(CONNECTION, null), (trc,v)->{
			var sgn = (FtpRequestSignal) trc;
			sgn.setProtocol("sftp");
			var cs = sftp.getSession(); //throws JSchException
			if(nonNull(cs)) {
				sgn.setHost(cs.getHost());
				sgn.setPort(cs.getPort());
				sgn.setUser(cs.getUserName());
				sgn.setServerVersion(cs.getServerVersion());
				sgn.setClientVersion(cs.getClientVersion());
			}
		}); //before end if thrw
	}
	
	public ExecutionListener<Void> disconnectionListener() {
		return disconnectionListener(stageBuilder(DISCONNECTION, null));
	}
	
	public <T> ExecutionListener<T> executeStageListener(FtpCommand cmd, String... args) {
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
