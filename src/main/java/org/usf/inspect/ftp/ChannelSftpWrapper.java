package org.usf.inspect.ftp;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.requestAppender;
import static org.usf.inspect.core.StageTracker.call;
import static org.usf.inspect.core.StageTracker.exec;
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

import org.usf.inspect.core.FtpRequest;
import org.usf.inspect.core.FtpRequestStage;
import org.usf.inspect.core.StageTracker.StageCreator;

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
		exec(channel::connect, this::toFtpRequest, requestAppender());
	}
	
	@Override
	public void connect(int connectTimeout) throws JSchException {
		exec(()-> channel.connect(connectTimeout), this::toFtpRequest, requestAppender());
	}
	
	@Override
	public void disconnect() {
		exec(channel::disconnect, ftpActionCreator(DISCONNECTION), this::appendDisconnection);
	}
	
	@Override
	public void quit() {
		exec(channel::quit, ftpActionCreator(DISCONNECTION), this::appendDisconnection);
	}
	
	@Override
	public void exit() {
		exec(channel::exit, ftpActionCreator(DISCONNECTION), this::appendDisconnection);
	}
	
	@Override
	public void get(String src, String dst) throws SftpException {
		exec(()-> channel.get(src, dst), ftpActionCreator(GET, src, dst), req::append);
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), ftpActionCreator(GET, src, dst), req::append);
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode), ftpActionCreator(GET, src, dst), req::append);
	}

	@Override
	public void get(String src, OutputStream dst) throws SftpException {
		exec(()-> channel.get(src, dst), ftpActionCreator(GET, src), req::append);
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), ftpActionCreator(GET, src), req::append);
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor, int mode, long skip) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode, skip), ftpActionCreator(GET, src), req::append);
	}

	@Override
	public InputStream get(String src) throws SftpException {
		return call(()-> channel.get(src), ftpActionCreator(GET, src), req::append);
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor) throws SftpException {
		return call(()-> channel.get(src, monitor), ftpActionCreator(GET, src), req::append);
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, int mode) throws SftpException {
		return call(()-> channel.get(src, mode), ftpActionCreator(GET, src), req::append);
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.get(src, monitor, mode), ftpActionCreator(GET, src), req::append);
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, long skip) throws SftpException {
		return call(()-> channel.get(src, monitor, skip), ftpActionCreator(GET, src), req::append);
	}
	
	@Override
	public Vector ls(String path) throws SftpException {
		return call(()-> channel.ls(path), ftpActionCreator(LS, path), req::append);
	}
	
	@Override
	public void ls(String path, LsEntrySelector selector) throws SftpException {
		exec(()-> channel.ls(path, selector), ftpActionCreator(LS, path), req::append);
	}
	
	/* write */

	@Override
	public void put(String src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), ftpActionCreator(PUT, src, dst), req::append);
	}

	@Override
	public void put(String src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), ftpActionCreator(PUT, src, dst), req::append);
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), ftpActionCreator(PUT, src, dst), req::append);
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), ftpActionCreator(PUT, src, dst), req::append);
	}

	@Override
	public void put(InputStream src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), ftpActionCreator(PUT, BYTES, dst), req::append);
	}

	@Override
	public void put(InputStream src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), ftpActionCreator(PUT, BYTES, dst), req::append);
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), ftpActionCreator(PUT, BYTES, dst), req::append);
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), ftpActionCreator(PUT, BYTES, dst), req::append);
	}

	@Override
	public void _put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel._put(src, dst, monitor, mode), ftpActionCreator(PUT, BYTES, dst), req::append);
	}

	@Override
	public OutputStream put(String dst) throws SftpException {
		return call(()-> channel.put(dst), ftpActionCreator(PUT, dst), req::append);
	}

	@Override
	public OutputStream put(String dst, int mode) throws SftpException {
		return call(()-> channel.put(dst, mode), ftpActionCreator(PUT, dst), req::append);
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode), ftpActionCreator(PUT, dst), req::append);
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode, long offset) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode, offset), ftpActionCreator(PUT, dst), req::append);
	}

	@Override
	public void mkdir(String path) throws SftpException {
		exec(()-> channel.mkdir(path), ftpActionCreator(MKDIR, path), req::append);
	}
	
	@Override
	public void rename(String oldpath, String newpath) throws SftpException {
		exec(()-> channel.rename(oldpath, newpath), ftpActionCreator(RENAME, oldpath, newpath), req::append);
	}
	
	@Override
	public void cd(String path) throws SftpException {
		exec(()-> channel.cd(path), ftpActionCreator(CD, path), req::append);
	}
	
	@Override
	public void chmod(int permissions, String path) throws SftpException {
		exec(()-> channel.chmod(permissions, path), ftpActionCreator(CHMOD, ""+permissions, path), req::append);
	}
	
	@Override
	public void chown(int uid, String path) throws SftpException {
		exec(()-> channel.chown(uid, path), ftpActionCreator(CHOWN, ""+uid, path), req::append);
	}

	@Override
	public void chgrp(int gid, String path) throws SftpException {
		exec(()-> channel.chgrp(gid, path), ftpActionCreator(CHGRP, ""+gid, path), req::append);
	}
	
	@Override
	public void rm(String path) throws SftpException {
		exec(()-> channel.rm(path), ftpActionCreator(RM, path), req::append);
	}
	
	@Override
	public void rmdir(String path) throws SftpException {
		exec(()-> channel.rmdir(path), ftpActionCreator(RM, path), req::append);
	}

	FtpRequest toFtpRequest(Instant start, Instant end, Void v, Throwable t) throws Exception {
		var cs = channel.getSession();
		req = new FtpRequest();
		req.setStart(start);
		if(nonNull(t)) {
			req.setEnd(end); //@see  appendDisconnection
		}
		req.setThreadName(threadName());
		req.setProtocol("ftps");
		req.setHost(cs.getHost());
		req.setPort(cs.getPort());
		req.setUser(cs.getUserName());
		req.setServerVersion(cs.getServerVersion());
		req.setClientVersion(cs.getClientVersion());
		req.setActions(new ArrayList<>());
		req.append(ftpActionCreator(CONNECTION).create(start, end, v, t));
		return req;
	}
	
	StageCreator<Object,FtpRequestStage> ftpActionCreator(FtpAction action, String... args) {
		return (s,e,o,t)-> {
			var stg = new FtpRequestStage();
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			stg.setArgs(args);
			if(nonNull(t)) {
				stg.setException(mainCauseException(t));
			}
			return stg;
		};
	}
	
	void appendDisconnection(FtpRequestStage stg) {
		req.append(stg);
		req.setEnd(stg.getEnd());
	}
	
	public static final ChannelSftpWrapper wrap(ChannelSftp channel) {
		return new ChannelSftpWrapper(channel);
	}
}
