package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestCommonStatus {

    public static final int CONN_ERROR   		= 0; // Generic I/O or Transport failure
    public static final int CONN_UNKNOWN_HOST  	= 1; // Host / DNS resolution failed
    public static final int CONN_REFUSED       	= 2; // Connection refused or unreachable
    public static final int CONN_INTERRUPTED   	= 3; // Connection interrupted or cancelled
    public static final int CONN_TIMEOUT       	= 4; // Connection establishment timeout

    public static final int SUCCESS            	= 200; // Success / OK

    public static final int CLIENT_ERROR       	= 400; // Generic client-side error
    public static final int CLIENT_UNAUTHORIZED = 401; // Authentication or permission failure
    public static final int CLIENT_TIMEOUT    	= 408; // Client-side operation timeout
    public static final int CLIENT_CONFLICT     = 409; // Duplicate key / Constraint violation

    public static final int SERVER_ERROR      	= 500; // Generic server/remote error
    public static final int SERVER_TIMEOUT     	= 504; // Server/Gateway response timeout
	
	public static int statusFor(Throwable t) {
	    if (t == null) {
	        return SUCCESS;
	    }
	    return switch (t) {

	        case java.net.UnknownHostException e -> CONN_UNKNOWN_HOST;
	        case java.nio.channels.UnresolvedAddressException e-> CONN_UNKNOWN_HOST;
	        case java.net.ConnectException e -> CONN_REFUSED;
	        case java.net.NoRouteToHostException e -> CONN_REFUSED;
	        case java.net.BindException e -> CONN_REFUSED;
	        
	        case java.net.SocketTimeoutException e -> nonNull(e.getMessage()) && e.getMessage().contains("connect") ? CONN_TIMEOUT : SERVER_TIMEOUT;
	        case java.util.concurrent.TimeoutException e -> CLIENT_TIMEOUT;
	        
	        case java.io.InterruptedIOException e -> CONN_INTERRUPTED;
	        case java.lang.InterruptedException e -> CONN_INTERRUPTED;
	        case java.util.concurrent.CancellationException e -> CONN_INTERRUPTED;
	        
	        case java.io.IOException e -> CONN_ERROR;

	        default -> SERVER_ERROR;
	    };
	}
	
}
