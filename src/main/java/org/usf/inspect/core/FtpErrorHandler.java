package org.usf.inspect.core;


import static org.usf.inspect.core.ErrorCode.CONNECTION_UNAVAILABLE;
import static org.usf.inspect.core.ErrorCode.TIMEOUT_OR_INTERRUPTION;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;

/**
 * Gestionnaire d’erreurs FTP/SFTP:
 * Convertit les exceptions réseau et JSch en codes d’erreur métier.
 *
 * @author Tasnim
 *
 */

public final class FtpErrorHandler implements ProtocolErrorHandler {

	@Override
	public int checkException(Throwable t) {
		return switch(t) {

			case java.net.UnknownHostException e->
				    CONNECTION_UNAVAILABLE.getCode();

			//timeout
			case java.net.SocketTimeoutException e->
					TIMEOUT_OR_INTERRUPTION.getCode();

			case java.net.SocketException e ->
					CONNECTION_UNAVAILABLE.getCode();

            case com.jcraft.jsch.JSchException e ->
					CONNECTION_UNAVAILABLE.getCode();

			// SFTP métier
			case com.jcraft.jsch.SftpException e ->
					e.id;

			default -> UNKNOWN_ERROR.getCode();
		};
	}

}
