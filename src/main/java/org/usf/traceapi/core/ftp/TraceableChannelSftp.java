package org.usf.traceapi.core.ftp;

import static java.util.Objects.isNull;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.stackTraceElement;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.Helper.warnNoActiveSession;
import static org.usf.traceapi.core.SafeSupplier.call;
import static org.usf.traceapi.core.SafeSupplier.supply;
import static org.usf.traceapi.core.ftp.FtpAction.CD;
import static org.usf.traceapi.core.ftp.FtpAction.CHGRP;
import static org.usf.traceapi.core.ftp.FtpAction.CHMOD;
import static org.usf.traceapi.core.ftp.FtpAction.CHOWN;
import static org.usf.traceapi.core.ftp.FtpAction.CONNECTION;
import static org.usf.traceapi.core.ftp.FtpAction.DISCONNECTION;
import static org.usf.traceapi.core.ftp.FtpAction.GET;
import static org.usf.traceapi.core.ftp.FtpAction.LS;
import static org.usf.traceapi.core.ftp.FtpAction.MKDIR;
import static org.usf.traceapi.core.ftp.FtpAction.PUT;
import static org.usf.traceapi.core.ftp.FtpAction.RENAME;
import static org.usf.traceapi.core.ftp.FtpAction.RM;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.usf.traceapi.core.FtpRequest;
import org.usf.traceapi.core.SafeSupplier.MetricsConsumer;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter(value = AccessLevel.PACKAGE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraceableChannelSftp extends ChannelSftp {
	
	private final ChannelSftp channel;
	private FtpRequest request;

	@Override
	public void connect() throws JSchException {
		call(channel::connect, this.appendConnection());
	}
	
	@Override
	public void connect(int connectTimeout) throws JSchException {
		call(()-> channel.connect(connectTimeout), this.appendConnection());
	}
	
	@Override
	public void disconnect() {
		call(channel::disconnect, this.appendDisconnection());
	}
	
	@Override
	public void quit() {
		call(channel::quit, this.appendDisconnection());
	}
	
	@Override
	public void exit() {
		call(channel::exit, this.appendDisconnection());
	}
	
	@Override
	public void get(String src, String dst) throws SftpException {
		call(()-> channel.get(src, dst), appendAction(GET));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		call(()-> channel.get(src, dst, monitor), appendAction(GET));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		call(()-> channel.get(src, dst, monitor, mode), appendAction(GET));
	}

	@Override
	public void get(String src, OutputStream dst) throws SftpException {
		call(()-> channel.get(src, dst), appendAction(GET));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor) throws SftpException {
		call(()-> channel.get(src, dst, monitor), appendAction(GET));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor, int mode, long skip) throws SftpException {
		call(()-> channel.get(src, dst, monitor, mode, skip), appendAction(GET));
	}

	@Override
	public InputStream get(String src) throws SftpException {
		return supply(()-> channel.get(src), appendAction(GET));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor) throws SftpException {
		return supply(()-> channel.get(src, monitor), appendAction(GET));
	}

	@Override
	public InputStream get(String src, int mode) throws SftpException {
		return supply(()-> channel.get(src, mode), appendAction(GET));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, int mode) throws SftpException {
		return supply(()-> channel.get(src, monitor, mode), appendAction(GET));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, long skip) throws SftpException {
		return supply(()-> channel.get(src, monitor, skip), appendAction(GET));
	}
	
	@Override
	public Vector ls(String path) throws SftpException {
		return supply(()-> channel.ls(path), appendAction(LS));
	}
	
	@Override
	public void ls(String path, LsEntrySelector selector) throws SftpException {
		call(()-> channel.ls(path, selector), appendAction(LS));
	}
	
	/* write */

	public void put(String src, String dst) throws SftpException {
		call(()-> channel.put(src, dst), appendAction(PUT));
	}

	public void put(String src, String dst, int mode) throws SftpException {
		call(()-> channel.put(src, dst, mode), appendAction(PUT));
	}

	public void put(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		call(()-> channel.put(src, dst, monitor), appendAction(PUT));
	}

	public void put(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		call(()-> channel.put(src, dst, monitor, mode), appendAction(PUT));
	}

	public void put(InputStream src, String dst) throws SftpException {
		call(()-> channel.put(src, dst), appendAction(PUT));
	}

	public void put(InputStream src, String dst, int mode) throws SftpException {
		call(()-> channel.put(src, dst, mode), appendAction(PUT));
	}

	public void put(InputStream src, String dst, SftpProgressMonitor monitor) throws SftpException {
		call(()-> channel.put(src, dst, monitor), appendAction(PUT));
	}

	public void put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		call(()-> channel.put(src, dst, monitor, mode), appendAction(PUT));
	}

	public OutputStream put(String dst) throws SftpException {
		return supply(()-> channel.put(dst), appendAction(PUT));
	}

	public OutputStream put(String dst, int mode) throws SftpException {
		return supply(()-> channel.put(dst, mode), appendAction(PUT));
	}

	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		return supply(()-> channel.put(dst, monitor, mode), appendAction(PUT));
	}

	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode, long offset) throws SftpException {
		return supply(()-> channel.put(dst, monitor, mode, offset), appendAction(PUT));
	}

	@Override
	public void mkdir(String path) throws SftpException {
		call(()-> channel.mkdir(path), appendAction(MKDIR));
	}
	
	@Override
	public void rename(String oldpath, String newpath) throws SftpException {
		call(()-> channel.rename(oldpath, newpath), appendAction(RENAME));
	}
	
	@Override
	public void cd(String path) throws SftpException {
		call(()-> channel.cd(path), appendAction(CD));
	}
	
	@Override
	public void chmod(int permissions, String path) throws SftpException {
		call(()-> channel.chmod(permissions, path), appendAction(CHMOD));
	}
	
	@Override
	public void chown(int uid, String path) throws SftpException {
		call(()-> channel.chown(uid, path), appendAction(CHOWN));
	}

	@Override
	public void chgrp(int gid, String path) throws SftpException {
		call(()-> channel.chgrp(gid, path), appendAction(CHGRP));
	}
	
	@Override
	public void rm(String path) throws SftpException {
		call(()-> channel.rm(path), appendAction(RM));
	}
	
	@Override
	public void rmdir(String path) throws SftpException {
		call(()-> channel.rmdir(path), appendAction(RM));
	}
	
	<T> MetricsConsumer<T> appendAction(FtpAction action) {
		return (s,e,o,ex)-> {
			var fa = new org.usf.traceapi.core.FtpAction();
			fa.setName(action.name());
			fa.setStart(s);
			fa.setEnd(e);
			fa.setException(ex);
			request.getActions().add(fa);
		};
	}
	
	MetricsConsumer<Void> appendConnection() {
		MetricsConsumer<Void> c = appendAction(CONNECTION);
		return c.thenAccept((s,e,o,ex)-> request.setStart(s));
	}
	
	MetricsConsumer<Void> appendDisconnection() {
		MetricsConsumer<Void> c = appendAction(DISCONNECTION);
		return c.thenAccept((s,e,o,ex)-> request.setEnd(e));
	}
	
	public static final ChannelSftp wrap(ChannelSftp channel) {
//		var session = localTrace.get();
//		if(isNull(session)) {
//			warnNoActiveSession();
//			return channel;
//		}
		try {
			var tcf = new TraceableChannelSftp(channel); // global | local
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
//			req.setHome(channel.getHome())
			tcf.setRequest(req);
//			session.append(req);
			return tcf;
		}
		catch (Exception e) {
			//do not throw exception
			return channel;
		}
		
	}
}
