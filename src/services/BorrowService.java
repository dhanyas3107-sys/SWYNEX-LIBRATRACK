package services;

import exceptions.BookNotAvailableException;
import exceptions.BookNotFoundException;
import exceptions.BorrowRecordNotFoundException;
import exceptions.BorrowingLimitExceededException;
import exceptions.InvalidInputException;
import exceptions.StudentNotFoundException;
import models.Book;
import models.BorrowRecord;
import models.Student;
import utils.LibraryConstants;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Issues and returns books and keeps the complete list of borrowing transactions. */
public class BorrowService {

    private final BookService bookService;
    private final StudentService studentService;
    private final FineService fineService;
    private final List<BorrowRecord> records = new ArrayList<>();
    private int nextTransactionId = LibraryConstants.FIRST_TRANSACTION_ID;

    public BorrowService(BookService bookService, StudentService studentService, FineService fineService) {
        this.bookService = bookService;
        this.studentService = studentService;
        this.fineService = fineService;
    }

    // ----- Issue -----

    /** Issues a book to a student today. */
    public BorrowRecord issueBook(int studentId, int bookId)
            throws StudentNotFoundException, BookNotFoundException, BookNotAvailableException,
                   BorrowingLimitExceededException, InvalidInputException {
        return issueBook(studentId, bookId, LocalDate.now());
    }

    /** Issues a book on an explicit date (used by the automated tests to create past transactions). */
    public BorrowRecord issueBook(int studentId, int bookId, LocalDate issueDate)
            throws StudentNotFoundException, BookNotFoundException, BookNotAvailableException,
                   BorrowingLimitExceededException, InvalidInputException {
        if (issueDate == null) {
            throw new InvalidInputException("Issue date is required.");
        }
        Student student = studentService.getStudentById(studentId);
        Book book = bookService.getBookById(bookId);

        if (!book.isAvailable()) {
            throw new BookNotAvailableException(bookId, book.getTitle());
        }
        int activeCount = countActiveRecordsForStudent(studentId);
        if (activeCount >= student.getBorrowLimit()) {
            throw new BorrowingLimitExceededException(studentId, student.getBorrowLimit());
        }

        LocalDate dueDate = issueDate.plusDays(LibraryConstants.LOAN_PERIOD_DAYS);
        BorrowRecord record = new BorrowRecord(nextTransactionId, student.getUserId(), student.getName(),
                book.getBookId(), book.getTitle(), issueDate, dueDate);
        book.markBorrowed();           // flips availability only after the record was built successfully
        records.add(record);
        nextTransactionId++;
        return record;
    }

    // ----- Return -----

    /** Returns a book today. */
    public BorrowRecord returnBook(int studentId, int bookId)
            throws StudentNotFoundException, BorrowRecordNotFoundException, InvalidInputException {
        return returnBook(studentId, bookId, LocalDate.now());
    }

    /** Returns a book on a given date and charges any overdue fine. */
    public BorrowRecord returnBook(int studentId, int bookId, LocalDate returnDate)
            throws StudentNotFoundException, BorrowRecordNotFoundException, InvalidInputException {
        if (returnDate == null) {
            throw new InvalidInputException("Return date is required.");
        }
        studentService.getStudentById(studentId);
        BorrowRecord record = findActiveRecord(studentId, bookId);

        int fine = fineService.calculateFine(record.getDueDate(), returnDate);
        record.markReturned(returnDate, fine);     // validates the date; throws before anything else changes
        bookService.findBookById(bookId).ifPresent(Book::markReturned);
        return record;
    }

    private BorrowRecord findActiveRecord(int studentId, int bookId) throws BorrowRecordNotFoundException {
        for (BorrowRecord record : records) {
            if (record.isActive() && record.getStudentId() == studentId && record.getBookId() == bookId) {
                return record;
            }
        }
        throw new BorrowRecordNotFoundException(studentId, bookId);
    }

    // ----- Queries -----

    public BorrowRecord getRecordById(int transactionId) throws BorrowRecordNotFoundException {
        for (BorrowRecord record : records) {
            if (record.getTransactionId() == transactionId) {
                return record;
            }
        }
        throw new BorrowRecordNotFoundException(transactionId);
    }

    public List<BorrowRecord> getAllRecords() {
        return Collections.unmodifiableList(records);
    }

    public List<BorrowRecord> getRecordsForStudent(int studentId) {
        List<BorrowRecord> result = new ArrayList<>();
        for (BorrowRecord record : records) {
            if (record.getStudentId() == studentId) {
                result.add(record);
            }
        }
        return result;
    }

    public List<BorrowRecord> getActiveRecords() {
        List<BorrowRecord> result = new ArrayList<>();
        for (BorrowRecord record : records) {
            if (record.isActive()) {
                result.add(record);
            }
        }
        return result;
    }

    public List<BorrowRecord> getCompletedRecords() {
        List<BorrowRecord> result = new ArrayList<>();
        for (BorrowRecord record : records) {
            if (record.isReturned()) {
                result.add(record);
            }
        }
        return result;
    }

    public List<BorrowRecord> getOverdueRecords(LocalDate asOf) {
        List<BorrowRecord> result = new ArrayList<>();
        for (BorrowRecord record : records) {
            if (record.isOverdue(asOf)) {
                result.add(record);
            }
        }
        return result;
    }

    public int countActiveRecordsForStudent(int studentId) {
        int count = 0;
        for (BorrowRecord record : records) {
            if (record.isActive() && record.getStudentId() == studentId) {
                count++;
            }
        }
        return count;
    }

    public boolean hasActiveRecordForBook(int bookId) {
        for (BorrowRecord record : records) {
            if (record.isActive() && record.getBookId() == bookId) {
                return true;
            }
        }
        return false;
    }

    public int getTotalTransactions() {
        return records.size();
    }

    public int getActiveCount() {
        return getActiveRecords().size();
    }

    // ----- Loading from storage -----

    /**
     * Installs records read from disk. Records with a repeated transaction ID are skipped,
     * and the ID counter is moved past the highest ID seen. Returns a warning per skipped record.
     */
    public List<String> restoreRecords(List<BorrowRecord> loaded) {
        List<String> warnings = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();
        records.clear();
        int highestId = LibraryConstants.FIRST_TRANSACTION_ID - 1;
        for (BorrowRecord record : loaded) {
            if (!seenIds.add(record.getTransactionId())) {
                warnings.add("Duplicate transaction " + record.getTransactionLabel() + " skipped.");
                continue;
            }
            records.add(record);
            highestId = Math.max(highestId, record.getTransactionId());
        }
        nextTransactionId = highestId + 1;
        return warnings;
    }
}
