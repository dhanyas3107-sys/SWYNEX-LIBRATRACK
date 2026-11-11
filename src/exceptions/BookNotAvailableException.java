package exceptions;

/** Thrown when a student tries to borrow a book that is already issued. */
public class BookNotAvailableException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public BookNotAvailableException(String message) {
        super(message);
    }

    public BookNotAvailableException(int bookId, String title) {
        super("\"" + title + "\" (Book ID " + bookId + ") is not available - it is already issued.");
    }
}
