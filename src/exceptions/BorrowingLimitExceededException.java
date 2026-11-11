package exceptions;

/** Thrown when a student already holds the maximum number of active books. */
public class BorrowingLimitExceededException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public BorrowingLimitExceededException(String message) {
        super(message);
    }

    public BorrowingLimitExceededException(int studentId, int limit) {
        super("Student " + studentId + " already has " + limit
                + " active book(s), which is the maximum allowed. Please return a book first.");
    }
}
