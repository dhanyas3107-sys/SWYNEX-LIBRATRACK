package exceptions;

/**
 * Base class for every checked, application-specific exception in LibraTrack.
 * Catching this single type lets the console layer handle any expected
 * library error and show a friendly message instead of a stack trace.
 */
public class LibraryException extends Exception {

    private static final long serialVersionUID = 1L;

    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
