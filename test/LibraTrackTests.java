import exceptions.BookNotAvailableException;
import exceptions.BookNotFoundException;
import exceptions.BorrowRecordNotFoundException;
import exceptions.BorrowingLimitExceededException;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.OperationNotAllowedException;
import exceptions.StudentNotFoundException;
import models.Book;
import models.BorrowRecord;
import models.Librarian;
import models.Student;
import models.User;
import services.BookService;
import services.LibraryService;
import storage.FileStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Dependency-free automated tests for LibraTrack (no JUnit jar required).
 *
 * Run:  java -cp out LibraTrackTests
 * Each scenario uses its own temporary data folder, so real data is never touched.
 */
public class LibraTrackTests {

    private static int passed = 0;
    private static int failed = 0;

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    // ----- Tiny assertion helpers -----

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    private static void expectThrows(String name, Class<? extends Exception> type, Action action) {
        try {
            action.run();
            failed++;
            System.out.println("  FAIL  " + name + " (no exception thrown)");
        } catch (Exception e) {
            if (type.isInstance(e)) {
                passed++;
                System.out.println("  PASS  " + name + "  -> " + e.getClass().getSimpleName());
            } else {
                failed++;
                System.out.println("  FAIL  " + name + " (threw " + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
            }
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title + " ==");
    }

    // ----- Fixtures -----

    private static Path newDataDir() throws IOException {
        return Files.createTempDirectory("libratrack-test-");
    }

    private static LibraryService newLibrary(Path dir) throws Exception {
        LibraryService library = new LibraryService(new FileStorage(dir));
        library.initialise();
        return library;
    }

    private static void seed(LibraryService library) throws Exception {
        library.addBook(1, "Clean Code", "Robert C. Martin", "Programming", "9780132350884", 2008);
        library.addBook(2, "Effective Java", "Joshua Bloch", "Programming", "9780134685991", 2018);
        library.addBook(3, "Introduction to Algorithms", "Thomas H. Cormen", "Algorithms", "9780262046305", 2022);
        library.addBook(4, "Head First Java", "Kathy Sierra", "Programming", "9781491910771", 2022);
        library.addBook(5, "Wings of Fire", "A. P. J. Abdul Kalam", "Autobiography", "9788173711466", 1999);
        library.registerStudent(101, "Arjun Kumar", "arjun@example.com", "9876543210", "Computer Science", 2);
        library.registerStudent(102, "Priya Sharma", "priya@example.com", "9123456780", "IT", 3);
    }

    private static void deleteRecursively(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // temp folder clean-up is best effort
        }
    }

    // ----- Entry point -----

    public static void main(String[] args) throws Exception {
        Path dir = newDataDir();
        try {
            testBooks(dir);
            testStudents(dir);
            testBorrowing();
            testReturnsAndFines();
            testPersistence();
            testOopAndValidation();
        } finally {
            deleteRecursively(dir);
        }
        System.out.println();
        System.out.println("=========================================");
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        System.out.println("=========================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ----- Scenarios 1, 2, 5, 15 -----

    private static void testBooks(Path ignored) throws Exception {
        section("Books: add, duplicate, search, update, delete (scenarios 1, 2, 5, 15)");
        Path dir = newDataDir();
        try {
            LibraryService library = newLibrary(dir);
            seed(library);

            check("1. Add a new book -> total is 5", library.getTotalBooks() == 5);
            expectThrows("2. Duplicate book ID is rejected", DuplicateRecordException.class,
                    () -> library.addBook(1, "Another", "Someone", "Misc", "9780132350884", 2020));
            expectThrows("   Invalid ISBN is rejected", InvalidInputException.class,
                    () -> library.addBook(50, "Bad ISBN", "Someone", "Misc", "12345", 2020));
            expectThrows("   Future publication year is rejected", InvalidInputException.class,
                    () -> library.addBook(51, "Future", "Someone", "Misc", "9780132350884", 3000));
            expectThrows("   Blank title is rejected", InvalidInputException.class,
                    () -> library.addBook(52, "   ", "Someone", "Misc", "9780132350884", 2020));
            check("   Rejected books were not stored", library.getTotalBooks() == 5);

            check("5. Search by ID finds Clean Code", library.searchBooks(1).get(0).getTitle().equals("Clean Code"));
            check("   Search by ID with unknown ID is empty", library.searchBooks(999).isEmpty());
            check("5. Search by title (case-insensitive, partial)",
                    library.searchBooks("java", BookService.SearchBy.TITLE).size() == 2);
            check("5. Search by author", library.searchBooks("bloch", BookService.SearchBy.AUTHOR).size() == 1);
            check("   Search by title AND author", library.searchBooks("java", "sierra").size() == 1);
            expectThrows("   Blank search keyword is rejected", InvalidInputException.class,
                    () -> library.searchBooks("  ", BookService.SearchBy.TITLE));

            library.updateBook(1, "Clean Code (2nd Ed)", "", "", "", 2009);
            Book updated = library.getBook(1);
            check("15. Update changes title and year", updated.getTitle().equals("Clean Code (2nd Ed)")
                    && updated.getPublicationYear() == 2009);
            check("    Update keeps blank fields unchanged", updated.getAuthor().equals("Robert C. Martin"));
            expectThrows("    Update with bad year changes nothing", InvalidInputException.class,
                    () -> library.updateBook(1, "Should Not Apply", null, null, null, 99));
            check("    ...title was not half-updated", library.getBook(1).getTitle().equals("Clean Code (2nd Ed)"));

            library.removeBook(5);
            check("15. Delete removes the book", library.getTotalBooks() == 4 && library.searchBooks(5).isEmpty());
            expectThrows("    Delete unknown book", BookNotFoundException.class, () -> library.removeBook(5));

            library.issueBook(101, 2);
            expectThrows("15. A borrowed book cannot be deleted", OperationNotAllowedException.class,
                    () -> library.removeBook(2));
            check("    ...and it is still in the catalogue", library.getTotalBooks() == 4);
        } finally {
            deleteRecursively(dir);
        }
    }

    // ----- Scenarios 3, 4 -----

    private static void testStudents(Path ignored) throws Exception {
        section("Students (scenarios 3, 4)");
        Path dir = newDataDir();
        try {
            LibraryService library = newLibrary(dir);
            seed(library);

            check("3. Register a student", library.getAllStudents().size() == 2);
            check("   Lookup by ID returns the student", library.getStudent(101).getName().equals("Arjun Kumar"));
            expectThrows("4. Duplicate student ID is rejected", DuplicateRecordException.class,
                    () -> library.registerStudent(101, "Clone", "clone@example.com", "9000000000", "CS", 1));
            expectThrows("   ID used by the librarian is rejected", DuplicateRecordException.class,
                    () -> library.registerStudent(9001, "Clash", "clash@example.com", "9000000000", "CS", 1));
            expectThrows("   Invalid email is rejected", InvalidInputException.class,
                    () -> library.registerStudent(103, "Bad Email", "not-an-email", "9000000000", "CS", 1));
            expectThrows("   Invalid contact number is rejected", InvalidInputException.class,
                    () -> library.registerStudent(103, "Bad Phone", "ok@example.com", "12ab", "CS", 1));
            expectThrows("   Unknown student lookup", StudentNotFoundException.class, () -> library.getStudent(999));
        } finally {
            deleteRecursively(dir);
        }
    }

    // ----- Scenarios 6, 7, 8 -----

    private static void testBorrowing() throws Exception {
        section("Issuing books (scenarios 6, 7, 8)");
        Path dir = newDataDir();
        try {
            LibraryService library = newLibrary(dir);
            seed(library);

            LocalDate issued = LocalDate.of(2026, 1, 1);
            BorrowRecord record = library.issueBook(101, 1, issued);
            check("6. Issue an available book", record.isActive());
            check("   Due date is borrow date + 14 days", record.getDueDate().equals(issued.plusDays(14)));
            check("   Book becomes unavailable", !library.getBook(1).isAvailable());
            check("   Transaction IDs start at 1001", record.getTransactionId() == 1001);

            expectThrows("7. Issuing an unavailable book fails", BookNotAvailableException.class,
                    () -> library.issueBook(102, 1));
            expectThrows("   Unknown book ID", BookNotFoundException.class, () -> library.issueBook(101, 999));
            expectThrows("   Unregistered student", StudentNotFoundException.class, () -> library.issueBook(999, 2));

            library.issueBook(101, 2);
            library.issueBook(101, 3);
            expectThrows("8. A 4th active book is refused (limit 3)", BorrowingLimitExceededException.class,
                    () -> library.issueBook(101, 4));
            check("   The refused book stays available", library.getBook(4).isAvailable());
            check("   Student holds exactly 3 books", library.countActiveBooksOfStudent(101) == 3);
        } finally {
            deleteRecursively(dir);
        }
    }

    // ----- Scenarios 9, 10, 11, 12 -----

    private static void testReturnsAndFines() throws Exception {
        section("Returns and fines (scenarios 9, 10, 11, 12)");
        Path dir = newDataDir();
        try {
            LibraryService library = newLibrary(dir);
            seed(library);
            LocalDate issued = LocalDate.of(2026, 1, 1);       // due 2026-01-15

            library.issueBook(101, 1, issued);
            BorrowRecord onTime = library.returnBook(101, 1, LocalDate.of(2026, 1, 10));
            check("9. Return a borrowed book", onTime.isReturned());
            check("12. Returned before due date -> fine 0", onTime.getFineAmount() == 0);
            check("    Book is available again", library.getBook(1).isAvailable());

            library.issueBook(101, 2, issued);
            BorrowRecord dueDay = library.returnBook(101, 2, LocalDate.of(2026, 1, 15));
            check("12. Returned exactly on the due date -> fine 0", dueDay.getFineAmount() == 0);

            library.issueBook(102, 3, issued);
            BorrowRecord late = library.returnBook(102, 3, LocalDate.of(2026, 2, 4));
            check("11. 20 days late -> 20 x 5 = 100", late.getFineAmount() == 100);
            check("    Return date is recorded", late.getReturnDate().equals(LocalDate.of(2026, 2, 4)));
            check("    1 day late -> 5", library.getFineService()
                    .calculateFine(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 16)) == 5);
            check("    Overdue days calculated with ChronoUnit", library.getFineService()
                    .calculateOverdueDays(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 4)) == 20);
            check("    Total fines generated = 100", library.getStatistics().totalFines() == 100);

            expectThrows("10. Returning a book never borrowed", BorrowRecordNotFoundException.class,
                    () -> library.returnBook(101, 4));
            expectThrows("    Returning the same book twice", BorrowRecordNotFoundException.class,
                    () -> library.returnBook(101, 1));
            expectThrows("    Return for unknown student", StudentNotFoundException.class,
                    () -> library.returnBook(999, 1));

            library.issueBook(101, 4, LocalDate.of(2026, 3, 1));
            expectThrows("    Return date before borrow date is rejected", InvalidInputException.class,
                    () -> library.returnBook(101, 4, LocalDate.of(2026, 2, 1)));
            check("    ...and the record is still active", library.getActiveRecords().size() == 1
                    && !library.getBook(4).isAvailable());
        } finally {
            deleteRecursively(dir);
        }
    }

    // ----- Scenarios 13, 14 -----

    private static void testPersistence() throws Exception {
        section("Persistence (scenarios 13, 14)");
        Path dir = newDataDir();
        try {
            LibraryService first = newLibrary(dir);
            seed(first);
            first.addBook(6, "Tricky | Title \\ with \"quotes\" & pipes", "O'Brien | Co", "Misc", "978-0-13-235088-4", 2015);
            first.registerStudent(103, "Zoë Müller | Jr", "zoe@example.com", "+91 98765 43210", "Física", 2);
            first.issueBook(101, 1, LocalDate.of(2026, 1, 1));
            first.issueBook(103, 6, LocalDate.of(2026, 2, 1));
            first.returnBook(103, 6, LocalDate.of(2026, 3, 1));   // 14 days late -> 70

            check("13. Data files were created", Files.exists(dir.resolve("books.txt"))
                    && Files.exists(dir.resolve("students.txt")) && Files.exists(dir.resolve("borrow_records.txt")));

            LibraryService second = newLibrary(dir);   // simulates restarting the application
            check("14. Books reloaded", second.getTotalBooks() == 6);
            check("14. Students reloaded", second.getAllStudents().size() == 3);
            check("14. Transactions reloaded", second.getAllRecords().size() == 2);
            check("14. Borrowed book is still unavailable", !second.getBook(1).isAvailable());
            check("14. Returned book is available", second.getBook(6).isAvailable());
            check("14. Fine history preserved", second.getStatistics().totalFines() == 70);
            check("    Special characters in title survive", second.getBook(6).getTitle()
                    .equals("Tricky | Title \\ with \"quotes\" & pipes"));
            check("    Special characters in author survive", second.getBook(6).getAuthor().equals("O'Brien | Co"));
            check("    ISBN normalised (hyphens removed)", second.getBook(6).getIsbn().equals("9780132350884"));
            check("    Non-ASCII student name survives", second.getStudent(103).getName().equals("Zoë Müller | Jr"));
            check("    Phone number normalised", second.getStudent(103).getContactNumber().equals("+919876543210"));
            check("    New transactions continue the ID sequence",
                    second.issueBook(102, 2).getTransactionId() == 1003);

            // Availability must be re-derived from the records even if the books file disagrees.
            Path booksFile = dir.resolve("books.txt");
            String text = Files.readString(booksFile, StandardCharsets.UTF_8);
            Files.writeString(booksFile, text.replace("1|Clean Code|Robert C. Martin|Programming|9780132350884|2008|false",
                    "1|Clean Code|Robert C. Martin|Programming|9780132350884|2008|true"), StandardCharsets.UTF_8);
            LibraryService third = newLibrary(dir);
            check("    Tampered availability is repaired from records", !third.getBook(1).isAvailable());
            check("    ...with a warning for the user", !third.getLoadWarnings().isEmpty());

            // A corrupted line must be skipped, not crash the load.
            Files.writeString(dir.resolve("students.txt"),
                    "# header\n104|Good Student|good@example.com|9000000001|CS|1\nthis line is garbage\n105|Bad|x|y|z|99\n",
                    StandardCharsets.UTF_8);
            LibraryService fourth = newLibrary(dir);
            check("    Corrupted lines are skipped", fourth.getAllStudents().size() == 1);
            check("    ...and reported", fourth.getLoadWarnings().size() >= 2);

            // Missing files / missing folder are created automatically.
            Path freshDir = dir.resolve("nested").resolve("data");
            LibraryService fresh = newLibrary(freshDir);
            check("    Missing data folder is created automatically", Files.isDirectory(freshDir)
                    && Files.exists(freshDir.resolve("books.txt")));
            check("    Fresh library starts empty", fresh.getTotalBooks() == 0);
        } finally {
            deleteRecursively(dir);
        }
    }

    // ----- OOP pillars -----

    private static void testOopAndValidation() throws Exception {
        section("OOP pillars and encapsulation checks");
        Student student = new Student(1, "Test Student", "t@example.com", "9000000000", "CS", 1);
        Librarian librarian = new Librarian(2, "Test Librarian", "l@example.com", "9000000001", "E-1", "Assistant");

        User a = student;      // parent-class references
        User b = librarian;
        check("Inheritance: Student is a User", a instanceof User && a instanceof Student);
        check("Inheritance: Librarian is a User", b instanceof User && b instanceof Librarian);
        check("Polymorphism: displayRole() differs", !a.displayRole().equals(b.displayRole()));
        check("Polymorphism: getUserType() differs", a.getUserType().equals("STUDENT") && b.getUserType().equals("LIBRARIAN"));
        check("Polymorphism: borrow limits differ", a.getBorrowLimit() == 3 && b.getBorrowLimit() == 0);
        check("Inheritance: subclass details extend super.getDetails()",
                a.getDetails().startsWith("ID: 1") && a.getDetails().contains("Department"));

        expectThrows("Encapsulation: setter validates e-mail", InvalidInputException.class,
                () -> student.setEmail("nope"));
        check("Encapsulation: invalid setter leaves state unchanged", student.getEmail().equals("t@example.com"));
        expectThrows("Encapsulation: invalid ID rejected in constructor", InvalidInputException.class,
                () -> new Student(0, "X", "x@example.com", "9000000000", "CS", 1));

        Book book = new Book(1, "T", "A", "C", "9780132350884", 2000);
        book.markBorrowed();
        expectThrows("Encapsulation: cannot borrow an already-borrowed book", BookNotAvailableException.class,
                book::markBorrowed);

        List<User> everyone = List.of(student, librarian);
        check("Collections of the parent type hold both subclasses", everyone.size() == 2);
    }
}
