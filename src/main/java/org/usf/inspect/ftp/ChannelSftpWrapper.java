package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.submit;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Vector;

import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
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
		exec(channel::connect, toFtpRequest());
	}
	
	@Override
	public void connect(int connectTimeout) throws JSchException {
		exec(()-> channel.connect(connectTimeout), toFtpRequest());
	}
	
	@Override
	public void disconnect() {
		exec(channel::disconnect, disconnection());
	}
	
	@Override
	public void quit() {
		exec(channel::quit, disconnection());
	}
	
	@Override
	public void exit() {
		exec(channel::exit, disconnection());
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

	ExecutionMonitorListener<Void> toFtpRequest() {
		req = new FtpRequest();
		return (s,e,o,t)->{ //safe block
			req.setThreadName(threadName());
			var cs = channel.getSession(); //broke session dependency
			req.setHost(cs.getHost());
			req.setPort(cs.getPort());
			req.setUser(cs.getUserName());
			req.setServerVersion(cs.getServerVersion());
			req.setClientVersion(cs.getClientVersion());
			submit(ses-> {
				req.setProtocol("sftp");
				req.setStart(s);
				if(nonNull(t)) { //if connection error
					req.setEnd(e);
				}
				req.setActions(new ArrayList<>(nonNull(t) ? 1 : 3));  //cnx, act, dec
				req.append(newStage(CONNECTION, s, e, t));
				ses.append(req);
			});
		};
	}

	ExecutionMonitorListener<Void> disconnection() {
		return (s,e,o,t)-> submit(ses-> {
			req.append(newStage(DISCONNECTION, s, e, t));
			req.setEnd(e);
		});
	}

	<T> ExecutionMonitorListener<T> ftpActionCreator(FtpAction action, String... args) {
		return (s,e,o,t)-> submit(ses-> req.append(newStage(action, s, e, t, args)));
	}
	
	static FtpRequestStage newStage(FtpAction action, Instant start, Instant end, Throwable t, String... args) {
		var stg = new FtpRequestStage();
		stg.setName(action.name());
		stg.setStart(start);
		stg.setEnd(end);
		stg.setArgs(args);
		if(nonNull(t)) {
			stg.setException(mainCauseException(t));
		}
		return stg;
	}
	
	public static final ChannelSftpWrapper wrap(ChannelSftp channel) {
		return new ChannelSftpWrapper(channel);
	}
}
