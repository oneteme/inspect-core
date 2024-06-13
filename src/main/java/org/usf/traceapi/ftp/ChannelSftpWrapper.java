package org.usf.traceapi.ftp;

import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.FtpRequest.newFtpRequest;
import static org.usf.traceapi.core.Helper.stackTraceElement;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.Session.appendSessionStage;
import static org.usf.traceapi.core.StageTracker.call;
import static org.usf.traceapi.core.StageTracker.exec;
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
import java.time.Instant;
import java.util.Vector;

import org.usf.traceapi.core.FtpRequest;
import org.usf.traceapi.core.FtpRequestStage;
import org.usf.traceapi.core.StageTracker.StageConsumer;

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
	private FtpRequest req = newFtpRequest(); //avoid nullPointer

	@Override
	public void connect() throws JSchException {
		exec(channel::connect, this::appendConnection);
	}
	
	@Override
	public void connect(int connectTimeout) throws JSchException {
		exec(()-> channel.connect(connectTimeout), this::appendConnection);
	}
	
	@Override
	public void disconnect() {
		exec(channel::disconnect, this::appendDisconnection);
	}
	
	@Override
	public void quit() {
		exec(channel::quit, this::appendDisconnection);
	}
	
	@Override
	public void exit() {
		exec(channel::exit, this::appendDisconnection);
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

	void appendConnection(Instant start, Instant end, Void o, Throwable t) throws Exception {
		var cs = channel.getSession();
		req = newFtpRequest();
		req.setStart(start);
		if(nonNull(t)) { // fail
			req.setEnd(end);
			//do not setException, already set in action
		}
		req.setHost(cs.getHost());
		req.setPort(cs.getPort());
		req.setUser(cs.getUserName());
		req.setThreadName(threadName());
		req.setServerVersion(cs.getServerVersion());
		req.setClientVersion(cs.getClientVersion());
		stackTraceElement().ifPresent(st->{
			req.setName(st.getMethodName());
			req.setLocation(st.getClassName());
		});
		appendAction(CONNECTION).accept(start, end, o, t);
		appendSessionStage(req);
	}
	
	void appendDisconnection(Instant start, Instant end, Void o, Throwable t) throws Exception {
		appendAction(DISCONNECTION).accept(start, end, o, t);
		req.setEnd(end);
	}

	<T> StageConsumer<T> appendAction(FtpAction action, String... args) {
		return (s,e,o,t)-> {
			var fa = new FtpRequestStage();
			fa.setName(action.name());
			fa.setStart(s);
			fa.setEnd(e);
			fa.setException(mainCauseException(t));
			fa.setArgs(args);
			req.getActions().add(fa);
		};
	}
	
	public static final ChannelSftp wrap(ChannelSftp channel) {
		return new ChannelSftpWrapper(channel);
	}
}
