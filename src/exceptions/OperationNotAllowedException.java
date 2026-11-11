package exceptions;

/** Thrown when a request is valid but breaks a library rule (e.g. deleting a borrowed book). */
public class OperationNotAllowedException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public OperationNotAllowedException(String message) {
        super(message);
    }
}
