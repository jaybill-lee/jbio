package org.jaybill.jbio.core;

public class JBIOException extends RuntimeException {

    public JBIOException(String message) {
        super(message);
    }

    public JBIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
