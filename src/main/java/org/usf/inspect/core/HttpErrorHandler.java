package org.usf.inspect.core;

import static org.usf.inspect.core.ErrorCode.CONNECTION_UNAVAILABLE;
import static org.usf.inspect.core.ErrorCode.TIMEOUT_OR_INTERRUPTION;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;
/**
 * Gestionnaire d’erreurs HTTP:
 * Convertit les exceptions réseau liées aux appels HTTP en codes d’erreur métier.
 *
 * @author Tasnim
 */
public class HttpErrorHandler implements ProtocolErrorHandler {

    @Override
    public int checkException(Throwable t) {
        return switch (t) {

            // Timeout reseau
            case java.net.http.HttpTimeoutException e-> TIMEOUT_OR_INTERRUPTION.getCode();

            // Timeout reseau
            case java.net.SocketTimeoutException e-> TIMEOUT_OR_INTERRUPTION.getCode();

            // Timeout reseau
            case java.util.concurrent.TimeoutException e -> TIMEOUT_OR_INTERRUPTION.getCode();

            // Thread interrompu
            case InterruptedException e-> TIMEOUT_OR_INTERRUPTION.getCode();


            case java.net.SocketException e -> CONNECTION_UNAVAILABLE.getCode();

            // DNS
            case java.net.UnknownHostException e-> CONNECTION_UNAVAILABLE.getCode();

            // Adresse invalide
            case java.nio.channels.UnresolvedAddressException e-> CONNECTION_UNAVAILABLE.getCode();

            default -> UNKNOWN_ERROR.getCode();
        };
    }

}