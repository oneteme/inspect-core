package org.usf.traceapi.ftp;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.stackTraceElement;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.Helper.warnNoActiveSession;
import static org.usf.traceapi.core.StageTracker.exec;
import static org.usf.traceapi.core.StageTracker.call;
import static org.usf.traceapi.ftp.FtpAction.CD;
import static org.usf.traceapi.ftp.FtpAction.CHGRP;
import static org.usf.traceapi.ftp.FtpAction.CHMOD;
import static org.usf.traceapi.ftp.FtpAction.CHOWN;
import static org.usf.traceapi.ftp.FtpAction.CONNECTION;
import static org.usf.traceapi.ftp.FtpAction.DISCONNECTION;
import static org.usf.traceapi.ftp.FtpAction.GET;
import static org.usf.traceapi.ftp.FtpAction.LS;
import static org.usf.traceapi.ftp.FtpAction.MKDIR;
import static org.usf.traceapi.ftp.FtpAction.PUT;
import static org.usf.traceapi.ftp.FtpAction.RENAME;
import static org.usf.traceapi.ftp.FtpAction.RM;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Vector;

import org.usf.traceapi.core.FtpRequest;
import org.usf.traceapi.core.FtpRequestStage;
import org.usf.traceapi.core.StageTracker.StageConsumer;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChannelSftpWrapper extends ChannelSftp {
	
	private static final String BYTES = "[BYTES]";
	
	private final ChannelSftp channel;
	private final FtpRequest request;

	@Override
	public void connect() throws JSchException {
		exec(channel::connect, this.appendConnection());
	}
	
	@Override
	public void connect(int connectTimeout) throws JSchException {
		exec(()-> channel.connect(connectTimeout), appendConnection());
	}
	
	@Override
	public void disconnect() {
		exec(channel::disconnect, appendDisconnection());
	}
	
	@Override
	public void quit() {
		exec(channel::quit, appendDisconnection());
	}
	
	@Override
	public void exit() {
		exec(channel::exit, appendDisconnection());
	}
	
	@Override
	public void get(String src, String dst) throws SftpException {
		exec(()-> channel.get(src, dst), appendAction(GET, src, dst));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), appendAction(GET, src, dst));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode), appendAction(GET, src, dst));
	}

	@Override
	public void get(String src, OutputStream dst) throws SftpException {
		exec(()-> channel.get(src, dst), appendAction(GET, src));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), appendAction(GET, src));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor, int mode, long skip) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode, skip), appendAction(GET, src));
	}

	@Override
	public InputStream get(String src) throws SftpException {
		return call(()-> channel.get(src), appendAction(GET, src));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor) throws SftpException {
		return call(()-> channel.get(src, monitor), appendAction(GET, src));
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, int mode) throws SftpException {
		return call(()-> channel.get(src, mode), appendAction(GET, src));
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.get(src, monitor, mode), appendAction(GET, src));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, long skip) throws SftpException {
		return call(()-> channel.get(src, monitor, skip), appendAction(GET, src));
	}
	
	@Override
	public Vector ls(String path) throws SftpException {
		return call(()-> channel.ls(path), appendAction(LS, path));
	}
	
	@Override
	public void ls(String path, LsEntrySelector selector) throws SftpException {
		exec(()-> channel.ls(path, selector), appendAction(LS, path));
	}
	
	/* write */

	@Override
	public void put(String src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), appendAction(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), appendAction(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), appendAction(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), appendAction(PUT, src, dst));
	}

	@Override
	public void put(InputStream src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), appendAction(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), appendAction(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), appendAction(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), appendAction(PUT, BYTES, dst));
	}

	@Override
	public void _put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel._put(src, dst, monitor, mode), appendAction(PUT, BYTES, dst));
	}

	@Override
	public OutputStream put(String dst) throws SftpException {
		return call(()-> channel.put(dst), appendAction(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, int mode) throws SftpException {
		return call(()-> channel.put(dst, mode), appendAction(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode), appendAction(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode, long offset) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode, offset), appendAction(PUT, dst));
	}

	@Override
	public void mkdir(String path) throws SftpException {
		exec(()-> channel.mkdir(path), appendAction(MKDIR, path));
	}
	
	@Override
	public void rename(String oldpath, String newpath) throws SftpException {
		exec(()-> channel.rename(oldpath, newpath), appendAction(RENAME, oldpath, newpath));
	}
	
	@Override
	public void cd(String path) throws SftpException {
		exec(()-> channel.cd(path), appendAction(CD, path));
	}
	
	@Override
	public void chmod(int permissions, String path) throws SftpException {
		exec(()-> channel.chmod(permissions, path), appendAction(CHMOD, ""+permissions, path));
	}
	
	@Override
	public void chown(int uid, String path) throws SftpException {
		exec(()-> channel.chown(uid, path), appendAction(CHOWN, ""+uid, path));
	}

	@Override
	public void chgrp(int gid, String path) throws SftpException {
		exec(()-> channel.chgrp(gid, path), appendAction(CHGRP, ""+gid, path));
	}
	
	@Override
	public void rm(String path) throws SftpException {
		exec(()-> channel.rm(path), appendAction(RM, path));
	}
	
	@Override
	public void rmdir(String path) throws SftpException {
		exec(()-> channel.rmdir(path), appendAction(RM, path));
	}
	
	StageConsumer<Void> appendConnection() {
		request.setStart(now());
		return appendAction(CONNECTION);
	}
	
	StageConsumer<Void> appendDisconnection() {
		try {
			return appendAction(DISCONNECTION);
		}
		finally {
			request.setEnd(now());
		}
	}
	
	<T> StageConsumer<T> appendAction(FtpAction action, String... args) {
		return (s,e,o,t)-> {
			var fa = new FtpRequestStage();
			fa.setName(action.name());
			fa.setStart(s);
			fa.setEnd(e);
			fa.setException(mainCauseException(t));
			fa.setArgs(args);
			request.getActions().add(fa);
		};
	}
	
	public static final ChannelSftp wrap(ChannelSftp channel) {
		var session = localTrace.get();
		if(isNull(session)) {
			warnNoActiveSession();
			return channel;
		}
		try {
			var ses = channel.getSession();
			var req = new FtpRequest();
			req.setHost(ses.getHost());
			req.setPort(ses.getPort());
			req.setServerVersion(ses.getServerVersion());
			req.setClientVersion(ses.getClientVersion());
			stackTraceElement().ifPresent(s->{
				req.setName(s.getMethodName());
				req.setLocation(s.getClassName());
			});
			req.setThreadName(threadName());
			req.setUser(ses.getUserName());
			req.setActions(new LinkedList<>());
//			req.setHome(channel.getHome())
			session.append(req);
			return new ChannelSftpWrapper(channel, req);
		}
		catch (Exception e) {
			//do not throw exception
			return channel;
		}
	}
}
