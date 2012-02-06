package com.orchid.net.server.exceptions;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 7:54
 */
public class NetworkException extends RuntimeException{
    public NetworkException() {
    }

    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetworkException(Throwable cause) {
        super(cause);
    }
}
