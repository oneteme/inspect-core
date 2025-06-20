package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.appendStage;
import static org.usf.inspect.ftp.FtpAction.CD;
import static org.usf.inspect.ftp.FtpAction.CHGRP;
import static org.usf.inspect.ftp.FtpAction.CHMOD;
import static org.usf.inspect.ftp.FtpAction.CHOWN;
import static org.usf.inspect.ftp.FtpAction.CONNECTION;
import static org.usf.inspect.ftp.FtpAction.DISCONNECTION;
import static org.usf.inspect.ftp.FtpAction.GET;
import static org.usf.inspect.ftp.FtpAction.LS;
import static org.usf.inspect.ftp.FtpAction.MKDIR;
import static org.usf.inspect.ftp.FtpAction.PUT;
import static org.usf.inspect.ftp.FtpAction.RENAME;
import static org.usf.inspect.ftp.FtpAction.RM;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Vector;

import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorFactory;
import org.usf.inspect.core.FtpRequest;
import org.usf.inspect.core.FtpRequestStage;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChannelSftpWrapper extends ChannelSftp {
	
	private static final String BYTES = "[BYTES]";
	
	private final ChannelSftp channel;
	private FtpRequest req;

	@Override
	public void connect() throws JSchException {
		exec(channel::connect, this.toFtpRequest());
	}
	
	@Override
	public void connect(int connectTimeout) throws JSchException {
		exec(()-> channel.connect(connectTimeout), this.toFtpRequest());
	}
	
	@Override
	public void disconnect() {
		exec(channel::disconnect, ftpActionCreator(DISCONNECTION));
	}
	
	@Override
	public void quit() {
		exec(channel::quit, ftpActionCreator(DISCONNECTION));
	}
	
	@Override
	public void exit() {
		exec(channel::exit, ftpActionCreator(DISCONNECTION));
	}
	
	@Override
	public void get(String src, String dst) throws SftpException {
		exec(()-> channel.get(src, dst), ftpActionCreator(GET, src, dst));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), ftpActionCreator(GET, src, dst));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode), ftpActionCreator(GET, src, dst));
	}

	@Override
	public void get(String src, OutputStream dst) throws SftpException {
		exec(()-> channel.get(src, dst), ftpActionCreator(GET, src));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), ftpActionCreator(GET, src));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor, int mode, long skip) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode, skip), ftpActionCreator(GET, src));
	}

	@Override
	public InputStream get(String src) throws SftpException {
		return call(()-> channel.get(src), ftpActionCreator(GET, src));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor) throws SftpException {
		return call(()-> channel.get(src, monitor), ftpActionCreator(GET, src));
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, int mode) throws SftpException {
		return call(()-> channel.get(src, mode), ftpActionCreator(GET, src));
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.get(src, monitor, mode), ftpActionCreator(GET, src));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, long skip) throws SftpException {
		return call(()-> channel.get(src, monitor, skip), ftpActionCreator(GET, src));
	}
	
	@Override
	public Vector ls(String path) throws SftpException {
		return call(()-> channel.ls(path), ftpActionCreator(LS, path));
	}
	
	@Override
	public void ls(String path, LsEntrySelector selector) throws SftpException {
		exec(()-> channel.ls(path, selector), ftpActionCreator(LS, path));
	}
	
	/* write */

	@Override
	public void put(String src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), ftpActionCreator(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), ftpActionCreator(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), ftpActionCreator(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), ftpActionCreator(PUT, src, dst));
	}

	@Override
	public void put(InputStream src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), ftpActionCreator(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), ftpActionCreator(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), ftpActionCreator(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), ftpActionCreator(PUT, BYTES, dst));
	}

	@Override
	public void _put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel._put(src, dst, monitor, mode), ftpActionCreator(PUT, BYTES, dst));
	}

	@Override
	public OutputStream put(String dst) throws SftpException {
		return call(()-> channel.put(dst), ftpActionCreator(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, int mode) throws SftpException {
		return call(()-> channel.put(dst, mode), ftpActionCreator(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode), ftpActionCreator(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode, long offset) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode, offset), ftpActionCreator(PUT, dst));
	}

	@Override
	public void mkdir(String path) throws SftpException {
		exec(()-> channel.mkdir(path), ftpActionCreator(MKDIR, path));
	}
	
	@Override
	public void rename(String oldpath, String newpath) throws SftpException {
		exec(()-> channel.rename(oldpath, newpath), ftpActionCreator(RENAME, oldpath, newpath));
	}
	
	@Override
	public void cd(String path) throws SftpException {
		exec(()-> channel.cd(path), ftpActionCreator(CD, path));
	}
	
	@Override
	public void chmod(int permissions, String path) throws SftpException {
		exec(()-> channel.chmod(permissions, path), ftpActionCreator(CHMOD, ""+permissions, path));
	}
	
	@Override
	public void chown(int uid, String path) throws SftpException {
		exec(()-> channel.chown(uid, path), ftpActionCreator(CHOWN, ""+uid, path));
	}

	@Override
	public void chgrp(int gid, String path) throws SftpException {
		exec(()-> channel.chgrp(gid, path), ftpActionCreator(CHGRP, ""+gid, path));
	}
	
	@Override
	public void rm(String path) throws SftpException {
		exec(()-> channel.rm(path), ftpActionCreator(RM, path));
	}
	
	@Override
	public void rmdir(String path) throws SftpException {
		exec(()-> channel.rmdir(path), ftpActionCreator(RM, path));
	}

	ExecutionMonitorFactory<Void> toFtpRequest() throws JSchException {
		req = new FtpRequest();
		return s-> { //safe block
			req.setStart(s);
			req.setProtocol("ftps");
			req.setThreadName(threadName());
			var cs = channel.getSession();
			req.setHost(cs.getHost());
			req.setPort(cs.getPort());
			req.setUser(cs.getUserName());
			req.setServerVersion(cs.getServerVersion());
			req.setClientVersion(cs.getClientVersion());
			req.setActions(new ArrayList<>(3));  //cnx, act, dec
			appendStage(req);
			ExecutionMonitorFactory<Void> stg = ftpActionCreator(CONNECTION);
			return stg.get(s);
		};
	}

	<T> ExecutionMonitorFactory<T> ftpActionCreator(FtpAction action, String... args) {
		return s-> {
			var stg = new FtpRequestStage();
			stg.setStart(s);
			stg.setName(action.name());
			stg.setArgs(args);
			req.append(stg); //at after setting
			return (e,v,t)-> {
				if(nonNull(t)) {
					stg.setException(mainCauseException(t));
				}
				stg.setEnd(e);
				if(action == DISCONNECTION || (action == CONNECTION && nonNull(t))) {
					req.setEnd(e);
				}
			};
		};
	}
	
	public static final ChannelSftpWrapper wrap(ChannelSftp channel) {
		return new ChannelSftpWrapper(channel);
	}
}
