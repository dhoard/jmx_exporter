package io.prometheus.jmx.test.support;

/** Class to implement UncheckedIOException */
public class UncheckedIOException extends RuntimeException {

    /**
     * Constructor
     *
     * @param message message
     */
    public UncheckedIOException(String message) {
        super(message);
    }

    /**
     * Constructor
     *
     * @param message message
     * @param throwable throwable
     */
    public UncheckedIOException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
