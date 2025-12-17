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
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.InspectExecutor.exec;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import lombok.AccessLevel;
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
	private FtpRequestMonitor monitor;

	@Override
	public void connect() throws JSchException {
		this.monitor = new FtpRequestMonitor();
		exec(channel::connect, monitor.handleConnection(channel));
	}
	
	@Override
	public void connect(int connectTimeout) throws JSchException {
		this.monitor = new FtpRequestMonitor();
		exec(()-> channel.connect(connectTimeout), monitor.handleConnection(channel));
	}
	
	@Override
	public void disconnect() {
		exec(channel::disconnect, monitor.handleDisconnection());
	}
	
	@Override
	public void quit() {
		exec(channel::quit, monitor.handleDisconnection());
	}
	
	@Override
	public void exit() {
		exec(channel::exit, monitor.handleDisconnection());
	}
	
	@Override
	public void get(String src, String dst) throws SftpException {
		exec(()-> channel.get(src, dst), monitor.executeStageHandler(GET, src, dst));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), this.monitor.executeStageHandler(GET, src, dst));
	}

	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode), this.monitor.executeStageHandler(GET, src, dst));
	}

	@Override
	public void get(String src, OutputStream dst) throws SftpException {
		exec(()-> channel.get(src, dst), monitor.executeStageHandler(GET, src));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), this.monitor.executeStageHandler(GET, src));
	}

	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor, int mode, long skip) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode, skip), this.monitor.executeStageHandler(GET, src));
	}

	@Override
	public InputStream get(String src) throws SftpException {
		return call(()-> channel.get(src), monitor.executeStageHandler(GET, src));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor) throws SftpException {
		return call(()-> channel.get(src, monitor), this.monitor.executeStageHandler(GET, src));
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, int mode) throws SftpException {
		return call(()-> channel.get(src, mode), monitor.executeStageHandler(GET, src));
	}

	/**
	 * @deprecated  This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.get(src, monitor, mode), this.monitor.executeStageHandler(GET, src));
	}

	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, long skip) throws SftpException {
		return call(()-> channel.get(src, monitor, skip),this. monitor.executeStageHandler(GET, src));
	}
	
	@Override
	public Vector ls(String path) throws SftpException {
		return call(()-> channel.ls(path), monitor.executeStageHandler(LS, path));
	}
	
	@Override
	public void ls(String path, LsEntrySelector selector) throws SftpException {
		exec(()-> channel.ls(path, selector), monitor.executeStageHandler(LS, path));
	}
	
	/* write */

	@Override
	public void put(String src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), monitor.executeStageHandler(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), monitor.executeStageHandler(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), this.monitor.executeStageHandler(PUT, src, dst));
	}

	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), this.monitor.executeStageHandler(PUT, src, dst));
	}

	@Override
	public void put(InputStream src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), monitor.executeStageHandler(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), monitor.executeStageHandler(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), this.monitor.executeStageHandler(PUT, BYTES, dst));
	}

	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), this.monitor.executeStageHandler(PUT, BYTES, dst));
	}

	@Override
	public void _put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel._put(src, dst, monitor, mode), this.monitor.executeStageHandler(PUT, BYTES, dst));
	}

	@Override
	public OutputStream put(String dst) throws SftpException {
		return call(()-> channel.put(dst), monitor.executeStageHandler(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, int mode) throws SftpException {
		return call(()-> channel.put(dst, mode), monitor.executeStageHandler(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode), this.monitor.executeStageHandler(PUT, dst));
	}

	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode, long offset) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode, offset), this.monitor.executeStageHandler(PUT, dst));
	}

	@Override
	public void mkdir(String path) throws SftpException {
		exec(()-> channel.mkdir(path), monitor.executeStageHandler(MKDIR, path));
	}
	
	@Override
	public void rename(String oldpath, String newpath) throws SftpException {
		exec(()-> channel.rename(oldpath, newpath), monitor.executeStageHandler(RENAME, oldpath, newpath));
	}
	
	@Override
	public void cd(String path) throws SftpException {
		exec(()-> channel.cd(path), monitor.executeStageHandler(CD, path));
	}
	
	@Override
	public void chmod(int permissions, String path) throws SftpException {
		exec(()-> channel.chmod(permissions, path), monitor.executeStageHandler(CHMOD, ""+permissions, path));
	}
	
	@Override
	public void chown(int uid, String path) throws SftpException {
		exec(()-> channel.chown(uid, path), monitor.executeStageHandler(CHOWN, ""+uid, path));
	}

	@Override
	public void chgrp(int gid, String path) throws SftpException {
		exec(()-> channel.chgrp(gid, path), monitor.executeStageHandler(CHGRP, ""+gid, path));
	}
	
	@Override
	public void rm(String path) throws SftpException {
		exec(()-> channel.rm(path), monitor.executeStageHandler(RM, path));
	}
	
	@Override
	public void rmdir(String path) throws SftpException {
		exec(()-> channel.rmdir(path), monitor.executeStageHandler(RM, path));
	}
	
	public static final ChannelSftp wrap(ChannelSftp channel) {
		return wrap(channel, null);
	}

	public static final ChannelSftp wrap(ChannelSftp channel, String beanName) {
		if(context().getConfiguration().isEnabled()){
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
