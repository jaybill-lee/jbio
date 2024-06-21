package org.jaybill.jbio.core.ex;

public class AcceptSocketChannelException extends RuntimeException {

    public AcceptSocketChannelException(String message) {
        super(message);
    }

    public AcceptSocketChannelException(String message, Throwable cause) {
        super(message, cause);
    }

    public AcceptSocketChannelException(Throwable cause) {
        super(cause);
    }
}
