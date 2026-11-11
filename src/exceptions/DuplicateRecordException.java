package exceptions;

/** Thrown when a book ID, user ID or transaction ID is already in use. */
public class DuplicateRecordException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public DuplicateRecordException(String message) {
        super(message);
    }
}
