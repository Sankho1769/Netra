package org.netra.core.exception;

public class EventRegistrationClosedException extends RuntimeException {
    public EventRegistrationClosedException(String message) {
        super(message);
    }
}
