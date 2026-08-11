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
 * Wraps an SFTP channel to trace file transfer and remote file management operations.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChannelSftpWrapper extends ChannelSftp {
	
	private static final String BYTES = "[BYTES]";
	
	private final ChannelSftp channel;
	private FtpRequestMonitor monitor;

	/**
	 * Connects the wrapped SFTP channel using its configured settings.
	 *
	 * @throws JSchException if the connection fails.
	 */
	@Override
	public void connect() throws JSchException {
		this.monitor = new FtpRequestMonitor();
		exec(channel::connect, monitor.connectionHandler(channel));
	}
	
	/**
	 * Connects the wrapped SFTP channel using the specified timeout.
	 *
	 * @param connectTimeout the connection timeout in milliseconds.
	 * @throws JSchException if the connection fails.
	 */
	@Override
	public void connect(int connectTimeout) throws JSchException {
		this.monitor = new FtpRequestMonitor();
		exec(()-> channel.connect(connectTimeout), monitor.connectionHandler(channel));
	}
	
	/**
	 * Disconnects the wrapped SFTP channel.
	 */
	@Override
	public void disconnect() {
		exec(channel::disconnect, monitor.disconnectionHandler());
	}
	
	/**
	 * Terminates the wrapped SFTP channel session.
	 */
	@Override
	public void quit() {
		exec(channel::quit, monitor.disconnectionHandler());
	}
	
	/**
	 * Exits the wrapped SFTP channel session.
	 */
	@Override
	public void exit() {
		exec(channel::exit, monitor.disconnectionHandler());
	}
	
	/**
	 * Downloads a remote file to a local destination.
	 *
	 * @param src the remote source path.
	 * @param dst the local destination path.
	 * @throws SftpException if the download fails.
	 */
	@Override
	public void get(String src, String dst) throws SftpException {
		exec(()-> channel.get(src, dst), monitor.executeStageHandler(GET, src, dst));
	}

	/**
	 * Downloads a remote file to a local destination while reporting progress.
	 *
	 * @param src the remote source path.
	 * @param dst the local destination path.
	 * @param monitor the progress monitor to notify.
	 * @throws SftpException if the download fails.
	 */
	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), this.monitor.executeStageHandler(GET, src, dst));
	}

	/**
	 * Downloads a remote file to a local destination using the specified transfer mode.
	 *
	 * @param src the remote source path.
	 * @param dst the local destination path.
	 * @param monitor the progress monitor to notify.
	 * @param mode the transfer mode to use.
	 * @throws SftpException if the download fails.
	 */
	@Override
	public void get(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode), this.monitor.executeStageHandler(GET, src, dst));
	}

	/**
	 * Downloads a remote file into an output stream.
	 *
	 * @param src the remote source path.
	 * @param dst the output stream that receives the file contents.
	 * @throws SftpException if the download fails.
	 */
	@Override
	public void get(String src, OutputStream dst) throws SftpException {
		exec(()-> channel.get(src, dst), monitor.executeStageHandler(GET, src));
	}

	/**
	 * Downloads a remote file into an output stream while reporting progress.
	 *
	 * @param src the remote source path.
	 * @param dst the output stream that receives the file contents.
	 * @param monitor the progress monitor to notify.
	 * @throws SftpException if the download fails.
	 */
	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.get(src, dst, monitor), this.monitor.executeStageHandler(GET, src));
	}

	/**
	 * Downloads a remote file into an output stream using the specified mode and skip offset.
	 *
	 * @param src the remote source path.
	 * @param dst the output stream that receives the file contents.
	 * @param monitor the progress monitor to notify.
	 * @param mode the transfer mode to use.
	 * @param skip the number of bytes to skip before writing.
	 * @throws SftpException if the download fails.
	 */
	@Override
	public void get(String src, OutputStream dst, SftpProgressMonitor monitor, int mode, long skip) throws SftpException {
		exec(()-> channel.get(src, dst, monitor, mode, skip), this.monitor.executeStageHandler(GET, src));
	}

	/**
	 * Opens an input stream for downloading a remote file.
	 *
	 * @param src the remote source path.
	 * @return an input stream for the remote file contents.
	 * @throws SftpException if the stream cannot be opened.
	 */
	@Override
	public InputStream get(String src) throws SftpException {
		return call(()-> channel.get(src), monitor.executeStageHandler(GET, src));
	}

	/**
	 * Opens an input stream for downloading a remote file while reporting progress.
	 *
	 * @param src the remote source path.
	 * @param monitor the progress monitor to notify.
	 * @return an input stream for the remote file contents.
	 * @throws SftpException if the stream cannot be opened.
	 */
	@Override
	public InputStream get(String src, SftpProgressMonitor monitor) throws SftpException {
		return call(()-> channel.get(src, monitor), this.monitor.executeStageHandler(GET, src));
	}

	/**
	 * Opens an input stream for downloading a remote file using the specified mode.
	 *
	 * @param src the remote source path.
	 * @param mode the transfer mode to use.
	 * @return an input stream for the remote file contents.
	 * @throws SftpException if the stream cannot be opened.
	 * @deprecated This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, int mode) throws SftpException {
		return call(()-> channel.get(src, mode), monitor.executeStageHandler(GET, src));
	}

	/**
	 * Opens an input stream for downloading a remote file with progress monitoring and the specified mode.
	 *
	 * @param src the remote source path.
	 * @param monitor the progress monitor to notify.
	 * @param mode the transfer mode to use.
	 * @return an input stream for the remote file contents.
	 * @throws SftpException if the stream cannot be opened.
	 * @deprecated This method will be deleted in the future.
	 */
	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.get(src, monitor, mode), this.monitor.executeStageHandler(GET, src));
	}

	/**
	 * Opens an input stream for downloading a remote file from the specified offset.
	 *
	 * @param src the remote source path.
	 * @param monitor the progress monitor to notify.
	 * @param skip the number of bytes to skip before reading.
	 * @return an input stream for the remote file contents.
	 * @throws SftpException if the stream cannot be opened.
	 */
	@Override
	public InputStream get(String src, SftpProgressMonitor monitor, long skip) throws SftpException {
		return call(()-> channel.get(src, monitor, skip),this. monitor.executeStageHandler(GET, src));
	}
	
	/**
	 * Lists the entries in the specified remote directory.
	 *
	 * @param path the remote directory path.
	 * @return the directory entries.
	 * @throws SftpException if the directory listing fails.
	 */
	@Override
	public Vector ls(String path) throws SftpException {
		return call(()-> channel.ls(path), monitor.executeStageHandler(LS, path));
	}
	
	/**
	 * Lists the entries in the specified remote directory using the provided selector.
	 *
	 * @param path the remote directory path.
	 * @param selector the entry selector that processes each directory entry.
	 * @throws SftpException if the directory listing fails.
	 */
	@Override
	public void ls(String path, LsEntrySelector selector) throws SftpException {
		exec(()-> channel.ls(path, selector), monitor.executeStageHandler(LS, path));
	}
	
	/* write */

	/**
	 * Uploads a local file to a remote destination.
	 *
	 * @param src the local source path.
	 * @param dst the remote destination path.
	 * @throws SftpException if the upload fails.
	 */
	@Override
	public void put(String src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), monitor.executeStageHandler(PUT, src, dst));
	}

	/**
	 * Uploads a local file to a remote destination using the specified mode.
	 *
	 * @param src the local source path.
	 * @param dst the remote destination path.
	 * @param mode the transfer mode to use.
	 * @throws SftpException if the upload fails.
	 */
	@Override
	public void put(String src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), monitor.executeStageHandler(PUT, src, dst));
	}

	/**
	 * Uploads a local file to a remote destination while reporting progress.
	 *
	 * @param src the local source path.
	 * @param dst the remote destination path.
	 * @param monitor the progress monitor to notify.
	 * @throws SftpException if the upload fails.
	 */
	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), this.monitor.executeStageHandler(PUT, src, dst));
	}

	/**
	 * Uploads a local file to a remote destination with progress monitoring and the specified mode.
	 *
	 * @param src the local source path.
	 * @param dst the remote destination path.
	 * @param monitor the progress monitor to notify.
	 * @param mode the transfer mode to use.
	 * @throws SftpException if the upload fails.
	 */
	@Override
	public void put(String src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), this.monitor.executeStageHandler(PUT, src, dst));
	}

	/**
	 * Uploads data from an input stream to a remote destination.
	 *
	 * @param src the input stream that provides the file contents.
	 * @param dst the remote destination path.
	 * @throws SftpException if the upload fails.
	 */
	@Override
	public void put(InputStream src, String dst) throws SftpException {
		exec(()-> channel.put(src, dst), monitor.executeStageHandler(PUT, BYTES, dst));
	}

	/**
	 * Uploads data from an input stream to a remote destination using the specified mode.
	 *
	 * @param src the input stream that provides the file contents.
	 * @param dst the remote destination path.
	 * @param mode the transfer mode to use.
	 * @throws SftpException if the upload fails.
	 */
	@Override
	public void put(InputStream src, String dst, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, mode), monitor.executeStageHandler(PUT, BYTES, dst));
	}

	/**
	 * Uploads data from an input stream to a remote destination while reporting progress.
	 *
	 * @param src the input stream that provides the file contents.
	 * @param dst the remote destination path.
	 * @param monitor the progress monitor to notify.
	 * @throws SftpException if the upload fails.
	 */
	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor) throws SftpException {
		exec(()-> channel.put(src, dst, monitor), this.monitor.executeStageHandler(PUT, BYTES, dst));
	}

	/**
	 * Uploads data from an input stream to a remote destination with progress monitoring and the specified mode.
	 *
	 * @param src the input stream that provides the file contents.
	 * @param dst the remote destination path.
	 * @param monitor the progress monitor to notify.
	 * @param mode the transfer mode to use.
	 * @throws SftpException if the upload fails.
	 */
	@Override
	public void put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel.put(src, dst, monitor, mode), this.monitor.executeStageHandler(PUT, BYTES, dst));
	}

	/**
	 * Uploads data from an input stream to a remote destination using the low-level put operation.
	 *
	 * @param src the input stream that provides the file contents.
	 * @param dst the remote destination path.
	 * @param monitor the progress monitor to notify.
	 * @param mode the transfer mode to use.
	 * @throws SftpException if the upload fails.
	 */
	@Override
	public void _put(InputStream src, String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		exec(()-> channel._put(src, dst, monitor, mode), this.monitor.executeStageHandler(PUT, BYTES, dst));
	}

	/**
	 * Opens an output stream for uploading to a remote destination.
	 *
	 * @param dst the remote destination path.
	 * @return an output stream for writing remote file contents.
	 * @throws SftpException if the stream cannot be opened.
	 */
	@Override
	public OutputStream put(String dst) throws SftpException {
		return call(()-> channel.put(dst), monitor.executeStageHandler(PUT, dst));
	}

	/**
	 * Opens an output stream for uploading to a remote destination using the specified mode.
	 *
	 * @param dst the remote destination path.
	 * @param mode the transfer mode to use.
	 * @return an output stream for writing remote file contents.
	 * @throws SftpException if the stream cannot be opened.
	 */
	@Override
	public OutputStream put(String dst, int mode) throws SftpException {
		return call(()-> channel.put(dst, mode), monitor.executeStageHandler(PUT, dst));
	}

	/**
	 * Opens an output stream for uploading to a remote destination with progress monitoring.
	 *
	 * @param dst the remote destination path.
	 * @param monitor the progress monitor to notify.
	 * @param mode the transfer mode to use.
	 * @return an output stream for writing remote file contents.
	 * @throws SftpException if the stream cannot be opened.
	 */
	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode), this.monitor.executeStageHandler(PUT, dst));
	}

	/**
	 * Opens an output stream for uploading to a remote destination from the specified offset.
	 *
	 * @param dst the remote destination path.
	 * @param monitor the progress monitor to notify.
	 * @param mode the transfer mode to use.
	 * @param offset the remote file offset to start writing at.
	 * @return an output stream for writing remote file contents.
	 * @throws SftpException if the stream cannot be opened.
	 */
	@Override
	public OutputStream put(String dst, SftpProgressMonitor monitor, int mode, long offset) throws SftpException {
		return call(()-> channel.put(dst, monitor, mode, offset), this.monitor.executeStageHandler(PUT, dst));
	}

	/**
	 * Creates a remote directory.
	 *
	 * @param path the remote directory path.
	 * @throws SftpException if the directory cannot be created.
	 */
	@Override
	public void mkdir(String path) throws SftpException {
		exec(()-> channel.mkdir(path), monitor.executeStageHandler(MKDIR, path));
	}
	
	/**
	 * Renames or moves a remote file or directory.
	 *
	 * @param oldpath the current remote path.
	 * @param newpath the new remote path.
	 * @throws SftpException if the rename fails.
	 */
	@Override
	public void rename(String oldpath, String newpath) throws SftpException {
		exec(()-> channel.rename(oldpath, newpath), monitor.executeStageHandler(RENAME, oldpath, newpath));
	}
	
	/**
	 * Changes the current remote directory.
	 *
	 * @param path the remote directory path.
	 * @throws SftpException if the directory cannot be changed.
	 */
	@Override
	public void cd(String path) throws SftpException {
		exec(()-> channel.cd(path), monitor.executeStageHandler(CD, path));
	}
	
	/**
	 * Changes the permissions of a remote file or directory.
	 *
	 * @param permissions the new permissions value.
	 * @param path the remote path to update.
	 * @throws SftpException if the permissions cannot be changed.
	 */
	@Override
	public void chmod(int permissions, String path) throws SftpException {
		exec(()-> channel.chmod(permissions, path), monitor.executeStageHandler(CHMOD, ""+permissions, path));
	}
	
	/**
	 * Changes the owner of a remote file or directory.
	 *
	 * @param uid the new user identifier.
	 * @param path the remote path to update.
	 * @throws SftpException if the owner cannot be changed.
	 */
	@Override
	public void chown(int uid, String path) throws SftpException {
		exec(()-> channel.chown(uid, path), monitor.executeStageHandler(CHOWN, ""+uid, path));
	}

	/**
	 * Changes the group of a remote file or directory.
	 *
	 * @param gid the new group identifier.
	 * @param path the remote path to update.
	 * @throws SftpException if the group cannot be changed.
	 */
	@Override
	public void chgrp(int gid, String path) throws SftpException {
		exec(()-> channel.chgrp(gid, path), monitor.executeStageHandler(CHGRP, ""+gid, path));
	}
	
	/**
	 * Removes a remote file.
	 *
	 * @param path the remote file path.
	 * @throws SftpException if the file cannot be removed.
	 */
	@Override
	public void rm(String path) throws SftpException {
		exec(()-> channel.rm(path), monitor.executeStageHandler(RM, path));
	}
	
	/**
	 * Removes a remote directory.
	 *
	 * @param path the remote directory path.
	 * @throws SftpException if the directory cannot be removed.
	 */
	@Override
	public void rmdir(String path) throws SftpException {
		exec(()-> channel.rmdir(path), monitor.executeStageHandler(RM, path));
	}
	
	/**
	 * Wraps the specified SFTP channel with Inspect monitoring.
	 *
	 * @param channel the SFTP channel to wrap.
	 * @return the wrapped channel.
	 */
	public static final ChannelSftp wrap(ChannelSftp channel) {
		return wrap(channel, null);
	}

	/**
	 * Wraps the specified SFTP channel with Inspect monitoring and an optional bean name.
	 *
	 * @param channel the SFTP channel to wrap.
	 * @param beanName the bean name used for logging, or {@code null} to use a default name.
	 * @return the wrapped channel, or the original channel when wrapping is skipped.
	 */
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
