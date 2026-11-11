package exceptions;

/** Thrown when no matching borrowing record exists (e.g. returning a book that was never borrowed). */
public class BorrowRecordNotFoundException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public BorrowRecordNotFoundException(String message) {
        super(message);
    }

    public BorrowRecordNotFoundException(int studentId, int bookId) {
        super("No active borrowing record found for student " + studentId + " and book " + bookId
                + ". The book was never borrowed by this student or has already been returned.");
    }

    public BorrowRecordNotFoundException(int transactionId) {
        super("No transaction found with ID " + transactionId + ".");
    }
}
