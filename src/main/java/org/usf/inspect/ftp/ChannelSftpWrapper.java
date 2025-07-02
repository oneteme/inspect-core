package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.startFtpRequest;
import static org.usf.inspect.core.SessionPublisher.emit;
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
		exec(channel::connect, sftpRequestListener());
	}
	
	@Override
	public void connect(int connectTimeout) throws JSchException {
		exec(()-> channel.connect(connectTimeout), sftpRequestListener());
	}
	
	@Override
	public void disconnect() {
		exec(channel::disconnect, closeListener());
	}
	
	@Override
	public void quit() {
		exec(channel::quit, closeListener());
	}
	
	@Override
	public void exit() {
		exec(channel::exit, closeListener());
	}
	
	@Override
	public void get(String src, String dst) throws SftpException {
		exec(()-> channel.get(src, dst), sftpStageListener(GET, src, dst));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), sftpStageListener(GET, src, dst));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode), sftpStageListener(GET, src, dst));
	}

	@Override
	public void get(String src, OutputStream dst) throws SftpException {
		exec(()-> channel.get(src, dst), sftpStageListener(GET, src));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), sftpStageListener(GET, src));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor, int mode, long skip) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode, skip), sftpStageListener(GET, src));
	}

	@Override
	public InputStream get(String src) throws SftpException {
		return call(()-> channel.get(src), sftpStageListener(GET, src));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor) throws SftpException {
		return call(()-> channel.get(src, monitor), sftpStageListener(GET, src));
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, int mode) throws SftpException {
		return call(()-> channel.get(src, mode), sftpStageListener(GET, src));
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.get(src, monitor, mode), sftpStageListener(GET, src));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, long skip) throws SftpException {
		return call(()-> channel.get(src, monitor, skip), sftpStageListener(GET, src));
	}
	
	@Override
	public Vector ls(String path) throws SftpException {
		return call(()-> channel.ls(path), sftpStageListener(LS, path));
	}
	
	@Override
	public void ls(String path, LsEntrySelector selector) throws SftpException {
		exec(()-> channel.ls(path, selector), sftpStageListener(LS, path));
	}
	
	/* write */

	@Override
	public void put(String src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), sftpStageListener(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), sftpStageListener(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), sftpStageListener(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), sftpStageListener(PUT, src, dst));
	}

	@Override
	public void put(InputStream src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), sftpStageListener(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), sftpStageListener(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), sftpStageListener(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), sftpStageListener(PUT, BYTES, dst));
	}

	@Override
	public void _put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel._put(src, dst, monitor, mode), sftpStageListener(PUT, BYTES, dst));
	}

	@Override
	public OutputStream put(String dst) throws SftpException {
		return call(()-> channel.put(dst), sftpStageListener(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, int mode) throws SftpException {
		return call(()-> channel.put(dst, mode), sftpStageListener(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode), sftpStageListener(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode, long offset) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode, offset), sftpStageListener(PUT, dst));
	}

	@Override
	public void mkdir(String path) throws SftpException {
		exec(()-> channel.mkdir(path), sftpStageListener(MKDIR, path));
	}
	
	@Override
	public void rename(String oldpath, String newpath) throws SftpException {
		exec(()-> channel.rename(oldpath, newpath), sftpStageListener(RENAME, oldpath, newpath));
	}
	
	@Override
	public void cd(String path) throws SftpException {
		exec(()-> channel.cd(path), sftpStageListener(CD, path));
	}
	
	@Override
	public void chmod(int permissions, String path) throws SftpException {
		exec(()-> channel.chmod(permissions, path), sftpStageListener(CHMOD, ""+permissions, path));
	}
	
	@Override
	public void chown(int uid, String path) throws SftpException {
		exec(()-> channel.chown(uid, path), sftpStageListener(CHOWN, ""+uid, path));
	}

	@Override
	public void chgrp(int gid, String path) throws SftpException {
		exec(()-> channel.chgrp(gid, path), sftpStageListener(CHGRP, ""+gid, path));
	}
	
	@Override
	public void rm(String path) throws SftpException {
		exec(()-> channel.rm(path), sftpStageListener(RM, path));
	}
	
	@Override
	public void rmdir(String path) throws SftpException {
		exec(()-> channel.rmdir(path), sftpStageListener(RM, path));
	}

	ExecutionMonitorListener<Void> closeListener() {
		return (s,e,o,t)->{
			emit(sftpStage(DISCONNECTION, s, e, t));
			req.lazy(()-> req.setEnd(e));
		};
	}

	ExecutionMonitorListener<Void> sftpRequestListener() {
		req = startFtpRequest();
		return (s,e,o,t)-> { //safe block
			req.setThreadName(threadName());
			req.setStart(s);
			if(nonNull(t)) { //if connection error
				req.setEnd(e);
			}
			req.setProtocol("sftp");
			var cs = channel.getSession(); //broke session dependency
			req.setHost(cs.getHost());
			req.setPort(cs.getPort());
			req.setUser(cs.getUserName());
			req.setServerVersion(cs.getServerVersion());
			req.setClientVersion(cs.getClientVersion());
			emit(req);
			emit(sftpStage(CONNECTION, s, e, t));
		};
	}

	<T> ExecutionMonitorListener<T> sftpStageListener(FtpAction action, String... args) {
		return (s,e,o,t)-> emit(sftpStage(action, s, e, t, args));
	}
	
	FtpRequestStage sftpStage(FtpAction action, Instant start, Instant end, Throwable t, String... args) {
		var stg = req.createStage(action.name(), start, end, t);
		stg.setArgs(args);
		return stg;
	}
	
	public static final ChannelSftpWrapper wrap(ChannelSftp channel) {
		return new ChannelSftpWrapper(channel);
	}
}
