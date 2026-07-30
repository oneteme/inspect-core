package org.usf.inspect.core;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class AuthUtils {

    private AuthUtils() {}

    /**
     * Génère la valeur du header Authorization: Basic Base64(namespace:token)
     */
    public static String buildBasicAuthHeader(String namespace, String token) {
        if (namespace == null) {
            return null;
        }
        String credentials = namespace + ":" + (token != null ? token : "");
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}