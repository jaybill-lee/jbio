package org.jaybill.jbio.core;

public class TaskExecutionException extends RuntimeException {
    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(Throwable cause) {
        super(cause);
    }
}
