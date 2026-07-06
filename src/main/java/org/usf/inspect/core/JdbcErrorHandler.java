package org.usf.inspect.core;

import java.sql.SQLException;

import static org.usf.inspect.core.ErrorCode.CONNECTION_UNAVAILABLE;
import static org.usf.inspect.core.ErrorCode.TIMEOUT_OR_INTERRUPTION;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;


/**
 * Gestionnaire d’erreurs JDBC:
 * Convertit les exceptions SQL et réseau liées à la base de données
 * en codes d’erreur métier exploitables.
 * Inclut également un mapping des SQLState et error codes.
 *
 * @author Tasnim
 */
public final class JdbcErrorHandler implements ProtocolErrorHandler {



	@Override
	public int checkException(Throwable t) {
		return switch (t) {

			case java.sql.SQLTimeoutException e->
					TIMEOUT_OR_INTERRUPTION.getCode();

			case java.net.SocketTimeoutException e ->
			       TIMEOUT_OR_INTERRUPTION.getCode();

			//à garder ou pas ??
			case java.io.EOFException e ->
					CONNECTION_UNAVAILABLE.getCode();

			case java.net.UnknownHostException e ->
					CONNECTION_UNAVAILABLE.getCode();

			case java.net.SocketException e ->
					CONNECTION_UNAVAILABLE.getCode();


			case InterruptedException e ->
			       TIMEOUT_OR_INTERRUPTION.getCode();

			case java.sql.SQLTransientConnectionException e ->
					CONNECTION_UNAVAILABLE.getCode();

			case java.sql.SQLNonTransientConnectionException e ->
					CONNECTION_UNAVAILABLE.getCode();

			case java.sql.SQLRecoverableException e ->
					CONNECTION_UNAVAILABLE.getCode();


			case SQLException e ->
					mapSqlException(e);

			default ->
					UNKNOWN_ERROR.getCode();

		};
	}






private int mapSqlException(SQLException e) {

    int errorCode = e.getErrorCode();
    String sqlState = e.getSQLState();

    // H2 / MySQL / Oracle utilisent souvent errorCode
    if (errorCode != 0) {
        return errorCode;
    }

    // PostgreSQL : souvent errorCode = 0
    if (sqlState != null) {
		try {
        // transformer le SQLState en code métier
		return Integer.parseInt(sqlState.substring(0, 2));
		} catch (NumberFormatException | IndexOutOfBoundsException ex) {
			return UNKNOWN_ERROR.getCode();
		}
    }

	return UNKNOWN_ERROR.getCode();
}

}
