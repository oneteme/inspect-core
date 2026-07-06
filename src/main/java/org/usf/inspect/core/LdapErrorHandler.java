package org.usf.inspect.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.usf.inspect.core.ErrorCode.CONNECTION_UNAVAILABLE;
import static org.usf.inspect.core.ErrorCode.TIMEOUT_OR_INTERRUPTION;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;

/**
 * Gestionnaire d’erreurs LDAP:
 * Convertit les exceptions JNDI en codes d’erreur métier exploitables.
 * Extrait également les codes LDAP présents dans les messages d’exception.
 * @author Tasnim
 */
public final class LdapErrorHandler implements ProtocolErrorHandler {

	@Override
	public int checkException(Throwable t) {
		return switch(t) {

			// serveur LDAP indisponible
			case javax.naming.ServiceUnavailableException e ->
					CONNECTION_UNAVAILABLE.getCode();
			//timeout
			case javax.naming.CommunicationException e ->
				TIMEOUT_OR_INTERRUPTION.getCode();


			case javax.naming.InterruptedNamingException e ->
				CONNECTION_UNAVAILABLE.getCode();

			// connexion OK mais erreur LDAP
			case javax.naming.NamingException e ->
				extractLdapCode(e);

			default -> UNKNOWN_ERROR.getCode();
		};
	}


	/**
	 * Extrait le code d'erreur LDAP (ex. "error code 49") du message de l'exception.
	 * La regex recherche "error code" suivi d'un ou plusieurs espaces, puis d'un nombre.
	 */
	private int extractLdapCode(javax.naming.NamingException e) {
		String msg = e.getMessage();

		if (msg != null) {
			Matcher m = Pattern.compile("error code\\s+(\\d+)").matcher(msg);

			if (m.find()) {
				return Integer.parseInt(m.group(1));
			}
		}

		return UNKNOWN_ERROR.getCode();
	}

}
