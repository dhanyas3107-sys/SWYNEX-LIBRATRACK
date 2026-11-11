package exceptions;

/** Thrown when user input or stored data fails validation. */
public class InvalidInputException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
