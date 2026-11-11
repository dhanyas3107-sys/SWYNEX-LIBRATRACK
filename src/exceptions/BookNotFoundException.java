package exceptions;

/** Thrown when a book ID does not exist in the library. */
public class BookNotFoundException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public BookNotFoundException(String message) {
        super(message);
    }

    public BookNotFoundException(int bookId) {
        super("No book found with ID " + bookId + ".");
    }
}
