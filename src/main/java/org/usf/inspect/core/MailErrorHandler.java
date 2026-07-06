package org.usf.inspect.core;

import jakarta.mail.MessagingException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.usf.inspect.core.ErrorCode.CONNECTION_UNAVAILABLE;
import static org.usf.inspect.core.ErrorCode.TIMEOUT_OR_INTERRUPTION;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;
import static org.usf.inspect.core.ErrorCode.AUTHENTIFICATION_ERROR;
/**
 * Gestionnaire d’erreurs Mail (SMTP/Jakarta Mail):
 * Convertit les exceptions liées aux emails en codes d’erreur métier
 * et extrait les codes SMTP présents dans les messages d’exception.
 *
 * @author Tasnim
 */
public final class MailErrorHandler implements ProtocolErrorHandler {


	/**
	 * Extrait un code SMTP à 3 chiffres du message.
	 */
	private static final Pattern SMTP_CODE =
	Pattern.compile("\\b\\d{3}\\b");

	@Override
	public int checkException(Throwable t) {
		return switch(t) {


			case java.io.InterruptedIOException e ->
					TIMEOUT_OR_INTERRUPTION.getCode();

			case java.net.UnknownHostException e ->
					CONNECTION_UNAVAILABLE.getCode();


			case java.net.SocketException e ->
					CONNECTION_UNAVAILABLE.getCode();

			case jakarta.mail.AuthenticationFailedException e ->
					AUTHENTIFICATION_ERROR.getCode();


			case jakarta.mail.MessagingException e ->
					extractSmtpCode(e);


			default ->
					UNKNOWN_ERROR.getCode();
		};
	}

	private int extractSmtpCode(MessagingException e) {

		String msg = e.getMessage();

		if (msg == null) {
			return CONNECTION_UNAVAILABLE.getCode();
		}

		Matcher matcher = SMTP_CODE.matcher(msg);

		if (matcher.find()) {
			try {
				return Integer.parseInt(matcher.group(1));
			} catch (NumberFormatException ex) {
				return UNKNOWN_ERROR.getCode();
			}
		}

		return UNKNOWN_ERROR.getCode();

	}

}
