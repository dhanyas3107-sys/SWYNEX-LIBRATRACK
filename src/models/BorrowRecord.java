package models;

import exceptions.InvalidInputException;
import utils.InputValidator;

import java.time.LocalDate;

/**
 * One borrowing transaction (ENCAPSULATION).
 *
 * <p>Student and book details are copied into the record so that history stays
 * readable even if a book is later removed. Dates and IDs are final; the only
 * way to change the record afterwards is {@link #markReturned(LocalDate, int)}.</p>
 */
public class BorrowRecord {

    /** Lifecycle of a transaction. */
    public enum Status { ACTIVE, RETURNED }

    private final int transactionId;
    private final int studentId;
    private final String studentName;
    private final int bookId;
    private final String bookTitle;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;   // null while the book is still out
    private int fineAmount;
    private Status status;

    /** Creates a brand-new ACTIVE record. */
    public BorrowRecord(int transactionId, int studentId, String studentName, int bookId,
                        String bookTitle, LocalDate borrowDate, LocalDate dueDate) throws InvalidInputException {
        this(transactionId, studentId, studentName, bookId, bookTitle,
                borrowDate, dueDate, null, 0, Status.ACTIVE);
    }

    /** Full constructor, also used when rebuilding a record from the data file. */
    public BorrowRecord(int transactionId, int studentId, String studentName, int bookId,
                        String bookTitle, LocalDate borrowDate, LocalDate dueDate,
                        LocalDate returnDate, int fineAmount, Status status) throws InvalidInputException {
        this.transactionId = InputValidator.validateId(transactionId, "Transaction ID");
        this.studentId = InputValidator.validateId(studentId, "Student ID");
        this.studentName = InputValidator.requireText(studentName, "Student name");
        this.bookId = InputValidator.validateId(bookId, "Book ID");
        this.bookTitle = InputValidator.requireText(bookTitle, "Book title");
        this.borrowDate = requireDate(borrowDate, "Borrowing date");
        this.dueDate = requireDate(dueDate, "Due date");
        if (dueDate.isBefore(borrowDate)) {
            throw new InvalidInputException("Due date cannot be before the borrowing date.");
        }
        if (fineAmount < 0) {
            throw new InvalidInputException("Fine amount cannot be negative.");
        }
        if (status == null) {
            throw new InvalidInputException("Transaction status is required.");
        }
        if (status == Status.RETURNED) {
            requireDate(returnDate, "Return date");
            if (returnDate.isBefore(borrowDate)) {
                throw new InvalidInputException("Return date cannot be before the borrowing date.");
            }
        } else if (returnDate != null) {
            throw new InvalidInputException("An ACTIVE transaction cannot have a return date.");
        }
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
        this.status = status;
    }

    private static LocalDate requireDate(LocalDate date, String fieldName) throws InvalidInputException {
        if (date == null) {
            throw new InvalidInputException(fieldName + " is required.");
        }
        return date;
    }

    // ----- Controlled state change -----

    /** Closes the transaction, recording the actual return date and the fine charged. */
    public void markReturned(LocalDate actualReturnDate, int fine) throws InvalidInputException {
        if (status == Status.RETURNED) {
            throw new InvalidInputException("Transaction " + getTransactionLabel() + " has already been returned.");
        }
        requireDate(actualReturnDate, "Return date");
        if (actualReturnDate.isBefore(borrowDate)) {
            throw new InvalidInputException("Return date (" + actualReturnDate
                    + ") cannot be before the borrowing date (" + borrowDate + ").");
        }
        if (fine < 0) {
            throw new InvalidInputException("Fine amount cannot be negative.");
        }
        this.returnDate = actualReturnDate;
        this.fineAmount = fine;
        this.status = Status.RETURNED;
    }

    // ----- Queries -----

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public boolean isReturned() {
        return status == Status.RETURNED;
    }

    /** True when the book is still out and the given day is after the due date. */
    public boolean isOverdue(LocalDate today) {
        return isActive() && today.isAfter(dueDate);
    }

    // ----- Getters -----

    public int getTransactionId() {
        return transactionId;
    }

    /** Display form such as TXN-1001. */
    public String getTransactionLabel() {
        return "TXN-" + transactionId;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public int getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public int getFineAmount() {
        return fineAmount;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return getTransactionLabel() + "[student " + studentId + ", book " + bookId + ", " + status + "]";
    }
}
