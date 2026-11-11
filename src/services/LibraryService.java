package services;

import exceptions.BookNotFoundException;
import exceptions.BorrowRecordNotFoundException;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.LibraryException;
import exceptions.StorageException;
import exceptions.StudentNotFoundException;
import models.Book;
import models.BorrowRecord;
import models.Librarian;
import models.Student;
import models.User;
import storage.FileStorage;
import utils.LibraryConstants;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The single entry point the console UI talks to (a facade over the specialised services).
 *
 * <p>It wires {@link BookService}, {@link StudentService}, {@link BorrowService} and
 * {@link FineService} together, loads data at start-up, and saves the affected data
 * files after every change so nothing is lost if the program is closed.</p>
 */
public class LibraryService {

    private final BookService bookService = new BookService();
    private final StudentService studentService = new StudentService();
    private final FineService fineService = new FineService();
    private final BorrowService borrowService = new BorrowService(bookService, studentService, fineService);
    private final FileStorage storage;
    private final List<Librarian> librarians = new ArrayList<>();
    private final List<String> loadWarnings = new ArrayList<>();

    public LibraryService(FileStorage storage) {
        this.storage = storage;
        try {
            librarians.add(new Librarian(LibraryConstants.DEFAULT_LIBRARIAN_ID,
                    LibraryConstants.DEFAULT_LIBRARIAN_NAME, LibraryConstants.DEFAULT_LIBRARIAN_EMAIL,
                    LibraryConstants.DEFAULT_LIBRARIAN_CONTACT, LibraryConstants.DEFAULT_LIBRARIAN_EMPLOYEE_ID,
                    LibraryConstants.DEFAULT_LIBRARIAN_DESIGNATION));
        } catch (InvalidInputException e) {
            // Only possible if the constants are edited incorrectly - fail loudly at start-up.
            throw new IllegalStateException("Default librarian constants are invalid: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // Start-up and persistence
    // =====================================================================

    /** Creates missing data files, then loads everything from disk. */
    public void initialise() throws StorageException {
        storage.initialise();
        loadData();
    }

    /**
     * Loads books, students and records, then re-derives each book's availability from the
     * active borrowing records so the two can never disagree after a restart.
     */
    public void loadData() throws StorageException {
        loadWarnings.clear();
        storage.clearWarnings();

        for (Book book : storage.loadBooks()) {
            try {
                bookService.addBook(book);
            } catch (DuplicateRecordException e) {
                loadWarnings.add("Book skipped while loading: " + e.getMessage());
            }
        }
        for (Student student : storage.loadStudents()) {
            try {
                if (isLibrarianId(student.getUserId())) {
                    throw new DuplicateRecordException("ID " + student.getUserId() + " belongs to a librarian.");
                }
                studentService.registerStudent(student);
            } catch (DuplicateRecordException e) {
                loadWarnings.add("Student skipped while loading: " + e.getMessage());
            }
        }
        loadWarnings.addAll(borrowService.restoreRecords(storage.loadRecords()));
        loadWarnings.addAll(storage.getWarnings());

        int corrected = reconcileBookAvailability();
        if (corrected > 0) {
            loadWarnings.add("Restored the availability of " + corrected
                    + " book(s) from the borrowing records.");
            storage.saveBooks(bookService.getAllBooks());
        }
    }

    /** Makes every book's availability match the active records. Returns how many books were fixed. */
    private int reconcileBookAvailability() {
        int corrected = 0;
        for (Book book : bookService.getAllBooks()) {
            boolean shouldBeBorrowed = borrowService.hasActiveRecordForBook(book.getBookId());
            if (shouldBeBorrowed && book.isAvailable()) {
                try {
                    book.markBorrowed();
                    corrected++;
                } catch (LibraryException e) {
                    // cannot happen: the book was just checked to be available
                    loadWarnings.add(e.getMessage());
                }
            } else if (!shouldBeBorrowed && !book.isAvailable()) {
                book.markReturned();
                corrected++;
            }
        }
        for (BorrowRecord record : borrowService.getActiveRecords()) {
            if (!bookService.bookIdExists(record.getBookId())) {
                loadWarnings.add("Active transaction " + record.getTransactionLabel()
                        + " refers to book ID " + record.getBookId() + ", which is not in the catalogue.");
            }
        }
        return corrected;
    }

    public List<String> getLoadWarnings() {
        return new ArrayList<>(loadWarnings);
    }

    /** Saves all three data files. */
    public void saveAll() throws StorageException {
        storage.saveBooks(bookService.getAllBooks());
        storage.saveStudents(studentService.getAllStudents());
        storage.saveRecords(borrowService.getAllRecords());
    }

    // =====================================================================
    // Books
    // =====================================================================

    public boolean bookIdExists(int bookId) {
        return bookService.bookIdExists(bookId);
    }

    public Book addBook(int bookId, String title, String author, String category, String isbn, int year)
            throws LibraryException {
        Book book = new Book(bookId, title, author, category, isbn, year);   // validates every field
        bookService.addBook(book);                                          // validates uniqueness
        storage.saveBooks(bookService.getAllBooks());
        return book;
    }

    public Book getBook(int bookId) throws BookNotFoundException {
        return bookService.getBookById(bookId);
    }

    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    public List<Book> getAvailableBooks() {
        return bookService.getAvailableBooks();
    }

    public List<Book> getBorrowedBooks() {
        return bookService.getBorrowedBooks();
    }

    public int getTotalBooks() {
        return bookService.getTotalBooks();
    }

    public List<Book> searchBooks(int bookId) {
        return bookService.searchBooks(bookId);
    }

    public List<Book> searchBooks(String keyword, BookService.SearchBy field) throws InvalidInputException {
        return bookService.searchBooks(keyword, field);
    }

    public List<Book> searchBooks(String title, String author) throws InvalidInputException {
        return bookService.searchBooks(title, author);
    }

    public Book updateBook(int bookId, String title, String author, String category, String isbn, Integer year)
            throws LibraryException {
        Book book = bookService.updateBook(bookId, title, author, category, isbn, year);
        storage.saveBooks(bookService.getAllBooks());
        return book;
    }

    public Book removeBook(int bookId) throws LibraryException {
        Book removed = bookService.removeBook(bookId);
        storage.saveBooks(bookService.getAllBooks());
        return removed;
    }

    // =====================================================================
    // Students and staff
    // =====================================================================

    private boolean isLibrarianId(int userId) {
        for (Librarian librarian : librarians) {
            if (librarian.getUserId() == userId) {
                return true;
            }
        }
        return false;
    }

    /** True if the ID is already used by a student or a librarian (user IDs are unique across both). */
    public boolean userIdExists(int userId) {
        return isLibrarianId(userId) || studentService.studentIdExists(userId);
    }

    public Student registerStudent(int studentId, String name, String email, String contact,
                                   String department, int yearOfStudy) throws LibraryException {
        Student student = new Student(studentId, name, email, contact, department, yearOfStudy);
        if (isLibrarianId(studentId)) {
            throw new DuplicateRecordException("ID " + studentId + " is already used by a librarian. User IDs must be unique.");
        }
        studentService.registerStudent(student);
        storage.saveStudents(studentService.getAllStudents());
        return student;
    }

    public Student getStudent(int studentId) throws StudentNotFoundException {
        return studentService.getStudentById(studentId);
    }

    public List<Student> getAllStudents() {
        return studentService.getAllStudents();
    }

    public Librarian getHeadLibrarian() {
        return librarians.get(0);
    }

    /** Every user (staff first, then students) as the common parent type {@link User}. */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>(librarians);
        users.addAll(studentService.getAllStudents());
        return users;
    }

    // =====================================================================
    // Borrowing and returning
    // =====================================================================

    public BorrowRecord issueBook(int studentId, int bookId) throws LibraryException {
        return issueBook(studentId, bookId, LocalDate.now());
    }

    public BorrowRecord issueBook(int studentId, int bookId, LocalDate issueDate) throws LibraryException {
        BorrowRecord record = borrowService.issueBook(studentId, bookId, issueDate);
        saveBooksAndRecords();
        return record;
    }

    public BorrowRecord returnBook(int studentId, int bookId) throws LibraryException {
        return returnBook(studentId, bookId, LocalDate.now());
    }

    public BorrowRecord returnBook(int studentId, int bookId, LocalDate returnDate) throws LibraryException {
        BorrowRecord record = borrowService.returnBook(studentId, bookId, returnDate);
        saveBooksAndRecords();
        return record;
    }

    private void saveBooksAndRecords() throws StorageException {
        storage.saveBooks(bookService.getAllBooks());
        storage.saveRecords(borrowService.getAllRecords());
    }

    // =====================================================================
    // History and fines
    // =====================================================================

    public BorrowRecord getRecord(int transactionId) throws BorrowRecordNotFoundException {
        return borrowService.getRecordById(transactionId);
    }

    public List<BorrowRecord> getAllRecords() {
        return borrowService.getAllRecords();
    }

    public List<BorrowRecord> getRecordsForStudent(int studentId) throws StudentNotFoundException {
        studentService.getStudentById(studentId);   // make sure the student exists
        return borrowService.getRecordsForStudent(studentId);
    }

    public List<BorrowRecord> getActiveRecords() {
        return borrowService.getActiveRecords();
    }

    public List<BorrowRecord> getCompletedRecords() {
        return borrowService.getCompletedRecords();
    }

    public List<BorrowRecord> getOverdueRecords(LocalDate asOf) {
        return borrowService.getOverdueRecords(asOf);
    }

    public int countActiveBooksOfStudent(int studentId) {
        return borrowService.countActiveRecordsForStudent(studentId);
    }

    public FineService getFineService() {
        return fineService;
    }

    /** Fines already charged on returned books. */
    public int getCollectedFines() {
        return fineService.getTotalFines(borrowService.getAllRecords());
    }

    /** Fines accrued so far on books that are still out and overdue. */
    public int getPendingFines(LocalDate asOf) {
        int total = 0;
        for (BorrowRecord record : borrowService.getOverdueRecords(asOf)) {
            total += fineService.calculateFine(record, asOf);
        }
        return total;
    }

    // =====================================================================
    // Statistics
    // =====================================================================

    public LibraryStatistics getStatistics() {
        return new LibraryStatistics(
                bookService.getTotalBooks(),
                bookService.getAvailableCount(),
                bookService.getBorrowedCount(),
                studentService.getTotalStudents(),
                borrowService.getTotalTransactions(),
                borrowService.getActiveCount(),
                getCollectedFines());
    }
}
