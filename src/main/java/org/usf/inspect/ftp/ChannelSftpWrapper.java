package org.usf.inspect.ftp;

import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.FtpCommand.CD;
import static org.usf.inspect.core.FtpCommand.CHGRP;
import static org.usf.inspect.core.FtpCommand.CHMOD;
import static org.usf.inspect.core.FtpCommand.CHOWN;
import static org.usf.inspect.core.FtpCommand.GET;
import static org.usf.inspect.core.FtpCommand.LS;
import static org.usf.inspect.core.FtpCommand.MKDIR;
import static org.usf.inspect.core.FtpCommand.PUT;
import static org.usf.inspect.core.FtpCommand.RENAME;
import static org.usf.inspect.core.FtpCommand.RM;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChannelSftpWrapper extends ChannelSftp {
	
	private static final String BYTES = "[BYTES]";
	
	private final ChannelSftp channel;
	private final FtpConnectionLifecycleTracer tracer = new FtpConnectionLifecycleTracer();

	@Override
	public void connect() throws JSchException {
		exec(channel::connect, tracer.connectionListener(channel));
	}
	
	@Override
	public void connect(int connectTimeout) throws JSchException {
		exec(()-> channel.connect(connectTimeout), tracer.connectionListener(channel));
	}
	
	@Override
	public void disconnect() {
		exec(channel::disconnect, tracer.disconnectionListener());
	}
	
	@Override
	public void quit() {
		exec(channel::quit, tracer.disconnectionListener());
	}
	
	@Override
	public void exit() {
		exec(channel::exit, tracer.disconnectionListener());
	}
	
	@Override
	public void get(String src, String dst) throws SftpException {
		exec(()-> channel.get(src, dst), tracer.executeStageListener(GET, src, dst));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), this.tracer.executeStageListener(GET, src, dst));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode), this.tracer.executeStageListener(GET, src, dst));
	}

	@Override
	public void get(String src, OutputStream dst) throws SftpException {
		exec(()-> channel.get(src, dst), tracer.executeStageListener(GET, src));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), this.tracer.executeStageListener(GET, src));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor, int mode, long skip) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode, skip), this.tracer.executeStageListener(GET, src));
	}

	@Override
	public InputStream get(String src) throws SftpException {
		return call(()-> channel.get(src), tracer.executeStageListener(GET, src));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor) throws SftpException {
		return call(()-> channel.get(src, monitor), this.tracer.executeStageListener(GET, src));
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, int mode) throws SftpException {
		return call(()-> channel.get(src, mode), tracer.executeStageListener(GET, src));
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.get(src, monitor, mode), this.tracer.executeStageListener(GET, src));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, long skip) throws SftpException {
		return call(()-> channel.get(src, monitor, skip),this. tracer.executeStageListener(GET, src));
	}
	
	@Override
	public Vector ls(String path) throws SftpException {
		return call(()-> channel.ls(path), tracer.executeStageListener(LS, path));
	}
	
	@Override
	public void ls(String path, LsEntrySelector selector) throws SftpException {
		exec(()-> channel.ls(path, selector), tracer.executeStageListener(LS, path));
	}
	
	/* write */

	@Override
	public void put(String src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), tracer.executeStageListener(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), tracer.executeStageListener(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), this.tracer.executeStageListener(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), this.tracer.executeStageListener(PUT, src, dst));
	}

	@Override
	public void put(InputStream src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), tracer.executeStageListener(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), tracer.executeStageListener(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), this.tracer.executeStageListener(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), this.tracer.executeStageListener(PUT, BYTES, dst));
	}

	@Override
	public void _put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel._put(src, dst, monitor, mode), this.tracer.executeStageListener(PUT, BYTES, dst));
	}

	@Override
	public OutputStream put(String dst) throws SftpException {
		return call(()-> channel.put(dst), tracer.executeStageListener(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, int mode) throws SftpException {
		return call(()-> channel.put(dst, mode), tracer.executeStageListener(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode), this.tracer.executeStageListener(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode, long offset) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode, offset), this.tracer.executeStageListener(PUT, dst));
	}

	@Override
	public void mkdir(String path) throws SftpException {
		exec(()-> channel.mkdir(path), tracer.executeStageListener(MKDIR, path));
	}
	
	@Override
	public void rename(String oldpath, String newpath) throws SftpException {
		exec(()-> channel.rename(oldpath, newpath), tracer.executeStageListener(RENAME, oldpath, newpath));
	}
	
	@Override
	public void cd(String path) throws SftpException {
		exec(()-> channel.cd(path), tracer.executeStageListener(CD, path));
	}
	
	@Override
	public void chmod(int permissions, String path) throws SftpException {
		exec(()-> channel.chmod(permissions, path), tracer.executeStageListener(CHMOD, ""+permissions, path));
	}
	
	@Override
	public void chown(int uid, String path) throws SftpException {
		exec(()-> channel.chown(uid, path), tracer.executeStageListener(CHOWN, ""+uid, path));
	}

	@Override
	public void chgrp(int gid, String path) throws SftpException {
		exec(()-> channel.chgrp(gid, path), tracer.executeStageListener(CHGRP, ""+gid, path));
	}
	
	@Override
	public void rm(String path) throws SftpException {
		exec(()-> channel.rm(path), tracer.executeStageListener(RM, path));
	}
	
	@Override
	public void rmdir(String path) throws SftpException {
		exec(()-> channel.rmdir(path), tracer.executeStageListener(RM, path));
	}
	
	public static final ChannelSftp wrap(ChannelSftp channel) {
		return wrap(channel, null);
	}

	public static final ChannelSftp wrap(@NonNull ChannelSftp channel, String beanName) {
		if(hub().getConfiguration().isEnabled()){
			if(channel.getClass() != ChannelSftpWrapper.class) {
				logWrappingBean(requireNonNullElse(beanName, "channelSftp"), channel.getClass());
				return new ChannelSftpWrapper(channel);
			}
			else { //will duplicate traces
				log.warn("{}: {} is already wrapped", beanName, channel);
			}
		}
		return channel;
	}
}
