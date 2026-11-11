package ui;

import exceptions.InvalidInputException;
import exceptions.LibraryException;
import exceptions.StorageException;
import models.Book;
import models.BorrowRecord;
import models.Student;
import models.User;
import services.BookService;
import services.FineService;
import services.LibraryService;
import services.LibraryStatistics;
import utils.InputValidator;
import utils.LibraryConstants;

import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive console front end. It only gathers input, calls {@link LibraryService}
 * and prints results; all business rules live in the services.
 *
 * <p>Every menu action runs inside {@link #runSafely(MenuAction)}, so a bad input or a
 * library rule violation prints a clear message and returns to the menu instead of
 * crashing. All input is read with {@code nextLine()} to avoid Scanner's newline pitfalls.</p>
 */
public class ConsoleUI {

    private static final String DOUBLE_LINE = "=".repeat(45);
    private static final String SINGLE_LINE = "-".repeat(45);

    // Table layouts (no trailing %n so the divider can match the header width exactly)
    private static final String BOOK_ROW = "%-5s %-26s %-18s %-14s %-13s %-4s %-9s";
    private static final String STUDENT_ROW = "%-5s %-22s %-28s %-13s %-20s %-4s";
    private static final String RECORD_ROW = "%-9s %-18s %-24s %-10s %-10s %-10s %-6s %-9s";
    private static final String OVERDUE_ROW = "%-9s %-18s %-24s %-10s %-8s %-8s";

    private static final int BACK_OPTION = 0;

    /** One menu action; returns true when the menu should be closed (back / exit). */
    @FunctionalInterface
    private interface MenuAction {
        boolean execute() throws LibraryException;
    }

    /** Raised when standard input ends (Ctrl+D / Ctrl+Z) so the program can save and exit cleanly. */
    private static final class InputClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private final LibraryService library;
    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleUI(LibraryService library, Scanner scanner, PrintStream out) {
        this.library = library;
        this.scanner = scanner;
        this.out = out;
    }

    // =====================================================================
    // Main loop
    // =====================================================================

    public void run() {
        printBanner();
        printLoadWarnings();
        try {
            boolean exit = false;
            while (!exit) {
                printMainMenu();
                exit = runSafely(() -> handleMainChoice(readMenuChoice(1, 8)));
            }
        } catch (InputClosedException e) {
            out.println();
            out.println("Input closed - saving data and exiting.");
        } finally {
            saveOnExit();
        }
        out.println("Thank you for using LibraTrack. Goodbye!");
    }

    private void saveOnExit() {
        try {
            library.saveAll();
        } catch (StorageException e) {
            out.println("[ERROR] Could not save data on exit: " + e.getMessage());
        }
    }

    /** Runs a menu action and converts any expected failure into a friendly message. */
    private boolean runSafely(MenuAction action) {
        try {
            return action.execute();
        } catch (LibraryException e) {
            printError(e.getMessage());
        } catch (InputClosedException e) {
            throw e;
        } catch (RuntimeException e) {
            printError("Something unexpected went wrong: " + e.getMessage());
        }
        return false;
    }

    private boolean handleMainChoice(int choice) {
        switch (choice) {
            case 1 -> bookManagementMenu();
            case 2 -> studentManagementMenu();
            case 3 -> runSafely(this::issueBook);
            case 4 -> runSafely(this::returnBook);
            case 5 -> borrowingHistoryMenu();
            case 6 -> fineManagementMenu();
            case 7 -> showStatistics();
            case 8 -> {
                return true;
            }
            default -> printError("Invalid choice.");
        }
        return false;
    }

    // =====================================================================
    // Screens: banner and menus
    // =====================================================================

    private void printBanner() {
        out.println(DOUBLE_LINE);
        out.println("               LIBRATRACK");
        out.println("        LIBRARY MANAGEMENT SYSTEM");
        out.println(DOUBLE_LINE);
        out.println();
        out.println("Welcome to LibraTrack!");
        out.println("Librarian on duty: " + library.getHeadLibrarian().getName());
        out.println();
    }

    private void printLoadWarnings() {
        List<String> warnings = library.getLoadWarnings();
        if (!warnings.isEmpty()) {
            out.println("[WARNING] Some stored data needed attention while loading:");
            for (String warning : warnings) {
                out.println("  - " + warning);
            }
            out.println();
        }
    }

    private void printMainMenu() {
        out.println(DOUBLE_LINE);
        out.println("MAIN MENU");
        out.println(DOUBLE_LINE);
        out.println("1. Book Management");
        out.println("2. Student Management");
        out.println("3. Issue a Book");
        out.println("4. Return a Book");
        out.println("5. Borrowing History");
        out.println("6. Fine Management");
        out.println("7. Library Statistics");
        out.println("8. Exit");
        out.println();
    }

    // =====================================================================
    // Book management
    // =====================================================================

    private void bookManagementMenu() {
        boolean back = false;
        while (!back) {
            printHeading("BOOK MANAGEMENT");
            out.println(" 1. Add a new book");
            out.println(" 2. View all books");
            out.println(" 3. Search by book ID");
            out.println(" 4. Search by title");
            out.println(" 5. Search by author");
            out.println(" 6. Update book details");
            out.println(" 7. Remove a book");
            out.println(" 8. View available books");
            out.println(" 9. View currently borrowed books");
            out.println("10. Display total number of books");
            out.println("11. Search by title AND author");
            out.println(" 0. Back to main menu");
            out.println();
            back = runSafely(() -> handleBookChoice(readMenuChoice(0, 11)));
        }
    }

    private boolean handleBookChoice(int choice) throws LibraryException {
        switch (choice) {
            case 1 -> addBook();
            case 2 -> printBookTable("ALL BOOKS", library.getAllBooks());
            case 3 -> searchBookById();
            case 4 -> searchBooksByField("title", BookService.SearchBy.TITLE);
            case 5 -> searchBooksByField("author", BookService.SearchBy.AUTHOR);
            case 6 -> updateBook();
            case 7 -> removeBook();
            case 8 -> printBookTable("AVAILABLE BOOKS", library.getAvailableBooks());
            case 9 -> printBookTable("CURRENTLY BORROWED BOOKS", library.getBorrowedBooks());
            case 10 -> out.println("Total number of books in the library: " + library.getTotalBooks());
            case 11 -> searchBooksByTitleAndAuthor();
            case BACK_OPTION -> {
                return true;
            }
            default -> printError("Invalid choice.");
        }
        out.println();
        return false;
    }

    private void addBook() throws LibraryException {
        printHeading("ADD A NEW BOOK");
        int bookId = promptInt("Book ID");
        InputValidator.validateId(bookId, "Book ID");
        if (library.bookIdExists(bookId)) {
            throw new exceptions.DuplicateRecordException("A book with ID " + bookId
                    + " already exists. Book IDs must be unique.");
        }
        String title = prompt("Title");
        String author = prompt("Author");
        String category = prompt("Category");
        String isbn = prompt("ISBN (10 or 13 digits)");
        int year = promptInt("Publication year");

        Book book = library.addBook(bookId, title, author, category, isbn, year);
        printSuccess("Book added: \"" + book.getTitle() + "\" (ID " + book.getBookId() + ").");
    }

    private void searchBookById() throws LibraryException {
        int bookId = promptInt("Enter book ID");
        printBookTable("SEARCH RESULT", library.searchBooks(bookId));
    }

    private void searchBooksByField(String label, BookService.SearchBy field) throws LibraryException {
        String keyword = prompt("Enter " + label + " (or part of it)");
        printBookTable("SEARCH RESULTS - " + label.toUpperCase(), library.searchBooks(keyword, field));
    }

    private void searchBooksByTitleAndAuthor() throws LibraryException {
        String title = prompt("Enter title (or part of it)");
        String author = prompt("Enter author (or part of it)");
        printBookTable("SEARCH RESULTS - TITLE AND AUTHOR", library.searchBooks(title, author));
    }

    private void updateBook() throws LibraryException {
        printHeading("UPDATE BOOK DETAILS");
        int bookId = promptInt("Book ID to update");
        Book book = library.getBook(bookId);
        out.println("Press Enter to keep the current value.");
        String title = prompt("Title [" + book.getTitle() + "]");
        String author = prompt("Author [" + book.getAuthor() + "]");
        String category = prompt("Category [" + book.getCategory() + "]");
        String isbn = prompt("ISBN [" + book.getIsbn() + "]");
        String yearText = prompt("Publication year [" + book.getPublicationYear() + "]");
        Integer year = InputValidator.isBlank(yearText) ? null : InputValidator.parseInt(yearText, "Publication year");

        Book updated = library.updateBook(bookId, title, author, category, isbn, year);
        printSuccess("Book " + updated.getBookId() + " updated successfully.");
        printBookTable("UPDATED BOOK", List.of(updated));
    }

    private void removeBook() throws LibraryException {
        printHeading("REMOVE A BOOK");
        int bookId = promptInt("Book ID to remove");
        Book book = library.getBook(bookId);
        if (!book.isAvailable()) {
            // Fail early instead of asking for confirmation first; the service enforces the same rule.
            throw new exceptions.OperationNotAllowedException("\"" + book.getTitle() + "\" (Book ID " + bookId
                    + ") is currently borrowed and cannot be removed. Return it first.");
        }
        if (!promptYesNo("Remove \"" + book.getTitle() + "\" permanently?")) {
            out.println("Removal cancelled.");
            return;
        }
        library.removeBook(bookId);
        printSuccess("Book \"" + book.getTitle() + "\" was removed.");
    }

    // =====================================================================
    // Student management
    // =====================================================================

    private void studentManagementMenu() {
        boolean back = false;
        while (!back) {
            printHeading("STUDENT MANAGEMENT");
            out.println("1. Register a new student");
            out.println("2. View all registered students");
            out.println("3. Search student by ID (display user information)");
            out.println("4. Display all library users and roles (polymorphism demo)");
            out.println("0. Back to main menu");
            out.println();
            back = runSafely(() -> handleStudentChoice(readMenuChoice(0, 4)));
        }
    }

    private boolean handleStudentChoice(int choice) throws LibraryException {
        switch (choice) {
            case 1 -> registerStudent();
            case 2 -> printStudentTable(library.getAllStudents());
            case 3 -> searchStudent();
            case 4 -> showAllUsers();
            case BACK_OPTION -> {
                return true;
            }
            default -> printError("Invalid choice.");
        }
        out.println();
        return false;
    }

    private void registerStudent() throws LibraryException {
        printHeading("REGISTER A NEW STUDENT");
        int studentId = promptInt("Student ID");
        InputValidator.validateId(studentId, "Student ID");
        if (library.userIdExists(studentId)) {
            throw new exceptions.DuplicateRecordException("User ID " + studentId
                    + " is already in use. User IDs must be unique.");
        }
        String name = prompt("Full name");
        String email = prompt("Email address");
        String contact = prompt("Contact number");
        String department = prompt("Department");
        int year = promptInt("Year of study (1-" + LibraryConstants.MAX_YEAR_OF_STUDY + ")");

        Student student = library.registerStudent(studentId, name, email, contact, department, year);
        printSuccess("Student registered: " + student.getName() + " (ID " + student.getUserId() + ").");
    }

    private void searchStudent() throws LibraryException {
        int studentId = promptInt("Enter student ID");
        Student student = library.getStudent(studentId);
        out.println(SINGLE_LINE);
        printField("Student ID", student.getUserId());
        printField("Name", student.getName());
        printField("Email", student.getEmail());
        printField("Contact", student.getContactNumber());
        printField("Department", student.getDepartment());
        printField("Year of study", student.getYearOfStudy());
        printField("Role", student.displayRole());
        printField("Active books", library.countActiveBooksOfStudent(studentId) + " of " + student.getBorrowLimit());
        out.println(SINGLE_LINE);
    }

    /**
     * Runtime polymorphism in action: the list holds the parent type {@code User}, yet each
     * call to displayRole()/getDetails() runs the Student or Librarian version at run time.
     */
    private void showAllUsers() {
        printHeading("ALL LIBRARY USERS");
        List<User> users = library.getAllUsers();
        for (User user : users) {
            out.println("[" + user.getUserType() + "] " + user.getName());
            out.println("   Role   : " + user.displayRole());
            out.println("   Details: " + user.getDetails());
            out.println("   Borrow limit: " + user.getBorrowLimit());
        }
        out.println(SINGLE_LINE);
        out.println(users.size() + " user(s) listed.");
    }

    // =====================================================================
    // Issue and return
    // =====================================================================

    private boolean issueBook() throws LibraryException {
        printHeading("ISSUE A BOOK");
        int studentId = promptInt("Student ID");
        int bookId = promptInt("Book ID");

        BorrowRecord record = library.issueBook(studentId, bookId);
        FineService fines = library.getFineService();
        printSuccess("Book issued successfully.");
        printField("Transaction", record.getTransactionLabel());
        printField("Student", record.getStudentName() + " (ID " + record.getStudentId() + ")");
        printField("Book", record.getBookTitle() + " (ID " + record.getBookId() + ")");
        printField("Borrowed on", record.getBorrowDate());
        printField("Due date", record.getDueDate());
        printField("Late fine", fines.formatAmount(fines.getFinePerDay()) + " per day after the due date");
        out.println();
        return false;
    }

    private boolean returnBook() throws LibraryException {
        printHeading("RETURN A BOOK");
        int studentId = promptInt("Student ID");
        int bookId = promptInt("Book ID");
        String dateText = prompt("Return date yyyy-MM-dd (press Enter for today)");

        BorrowRecord record;
        if (InputValidator.isBlank(dateText)) {
            record = library.returnBook(studentId, bookId);
        } else {
            record = library.returnBook(studentId, bookId, InputValidator.parseDate(dateText, "Return date"));
        }
        printSuccess("Book returned successfully.");
        printFineDetails(record, record.getReturnDate());
        out.println();
        return false;
    }

    // =====================================================================
    // Borrowing history
    // =====================================================================

    private void borrowingHistoryMenu() {
        boolean back = false;
        while (!back) {
            printHeading("BORROWING HISTORY");
            out.println("1. View all borrowing records");
            out.println("2. View history of a particular student");
            out.println("3. View active borrowing records");
            out.println("4. View completed transactions");
            out.println("0. Back to main menu");
            out.println();
            back = runSafely(() -> handleHistoryChoice(readMenuChoice(0, 4)));
        }
    }

    private boolean handleHistoryChoice(int choice) throws LibraryException {
        switch (choice) {
            case 1 -> printRecordTable("ALL BORROWING RECORDS", library.getAllRecords());
            case 2 -> {
                int studentId = promptInt("Student ID");
                Student student = library.getStudent(studentId);
                printRecordTable("HISTORY OF " + student.getName().toUpperCase(),
                        library.getRecordsForStudent(studentId));
            }
            case 3 -> printRecordTable("ACTIVE BORROWING RECORDS", library.getActiveRecords());
            case 4 -> printRecordTable("COMPLETED TRANSACTIONS", library.getCompletedRecords());
            case BACK_OPTION -> {
                return true;
            }
            default -> printError("Invalid choice.");
        }
        out.println();
        return false;
    }

    // =====================================================================
    // Fine management
    // =====================================================================

    private void fineManagementMenu() {
        boolean back = false;
        while (!back) {
            printHeading("FINE MANAGEMENT");
            out.println("Fine rule: " + library.getFineService().formatAmount(library.getFineService().getFinePerDay())
                    + " per day after the due date.");
            out.println("1. Check the fine for a transaction");
            out.println("2. View overdue books and fines accrued so far");
            out.println("3. Fine summary");
            out.println("0. Back to main menu");
            out.println();
            back = runSafely(() -> handleFineChoice(readMenuChoice(0, 3)));
        }
    }

    private boolean handleFineChoice(int choice) throws LibraryException {
        switch (choice) {
            case 1 -> {
                int transactionId = promptInt("Transaction ID (the number in TXN-xxxx)");
                BorrowRecord record = library.getRecord(transactionId);
                printFineDetails(record, LocalDate.now());
            }
            case 2 -> printOverdueTable();
            case 3 -> printFineSummary();
            case BACK_OPTION -> {
                return true;
            }
            default -> printError("Invalid choice.");
        }
        out.println();
        return false;
    }

    private void printFineDetails(BorrowRecord record, LocalDate asOf) {
        FineService fines = library.getFineService();
        int overdueDays = fines.calculateOverdueDays(record, asOf);
        int fine = fines.calculateFine(record, asOf);

        out.println(SINGLE_LINE);
        printField("Transaction", record.getTransactionLabel() + " (" + record.getStatus() + ")");
        printField("Student", record.getStudentName() + " (ID " + record.getStudentId() + ")");
        printField("Book", record.getBookTitle() + " (ID " + record.getBookId() + ")");
        printField("Borrowing date", record.getBorrowDate());
        printField("Due date", record.getDueDate());
        printField("Return date", record.isReturned() ? record.getReturnDate() : "Not returned yet");
        printField("Days overdue", overdueDays + (record.isActive() ? " (as of " + asOf + ")" : ""));
        if (record.isReturned()) {
            printField("Fine charged", fines.formatAmount(fine));
        } else {
            printField("Fine so far", fines.formatAmount(fine));
        }
        out.println(SINGLE_LINE);
        if (overdueDays == 0) {
            out.println("No fine applies - " + (record.isReturned() ? "returned" : "still within")
                    + " on or before the due date.");
        }
    }

    private void printOverdueTable() {
        LocalDate today = LocalDate.now();
        FineService fines = library.getFineService();
        List<BorrowRecord> overdue = library.getOverdueRecords(today);

        printHeading("OVERDUE BOOKS (as of " + today + ")");
        if (overdue.isEmpty()) {
            out.println("No books are overdue.");
            return;
        }
        String header = String.format(OVERDUE_ROW, "TXN", "Student", "Book", "Due date", "Days", "Fine");
        out.println(header);
        out.println("-".repeat(header.length()));
        for (BorrowRecord record : overdue) {
            out.println(String.format(OVERDUE_ROW, record.getTransactionLabel(),
                    fit(record.getStudentId() + " " + record.getStudentName(), 18),
                    fit(record.getBookId() + " " + record.getBookTitle(), 24),
                    record.getDueDate(), fines.calculateOverdueDays(record, today),
                    fines.formatAmount(fines.calculateFine(record, today))));
        }
        out.println("-".repeat(header.length()));
        out.println(overdue.size() + " overdue book(s).");
    }

    private void printFineSummary() {
        FineService fines = library.getFineService();
        LocalDate today = LocalDate.now();
        int withFines = 0;
        for (BorrowRecord record : library.getCompletedRecords()) {
            if (record.getFineAmount() > 0) {
                withFines++;
            }
        }
        printHeading("FINE SUMMARY");
        printField("Fine per day", fines.formatAmount(fines.getFinePerDay()));
        printField("Fines charged (returned)", fines.formatAmount(library.getCollectedFines()));
        printField("Returns with a fine", withFines);
        printField("Fines accruing (overdue)", fines.formatAmount(library.getPendingFines(today)) + " as of " + today);
        out.println(SINGLE_LINE);
    }

    // =====================================================================
    // Statistics
    // =====================================================================

    private boolean showStatistics() {
        LibraryStatistics stats = library.getStatistics();
        printHeading("LIBRARY STATISTICS");
        printStat("Total books", stats.totalBooks());
        printStat("Available books", stats.availableBooks());
        printStat("Borrowed books", stats.borrowedBooks());
        printStat("Registered students", stats.totalStudents());
        printStat("Total borrowing transactions", stats.totalTransactions());
        printStat("Active borrowing transactions", stats.activeTransactions());
        printStat("Total fines generated", library.getFineService().formatAmount(stats.totalFines()));
        out.println(SINGLE_LINE);
        out.println();
        return false;
    }

    private void printStat(String label, Object value) {
        out.printf("  %-32s: %s%n", label, value);
    }

    // =====================================================================
    // Tables
    // =====================================================================

    private void printBookTable(String title, List<Book> books) {
        printHeading(title);
        if (books.isEmpty()) {
            out.println("No books found.");
            return;
        }
        String header = String.format(BOOK_ROW, "ID", "Title", "Author", "Category", "ISBN", "Year", "Status");
        out.println(header);
        out.println("-".repeat(header.length()));
        for (Book book : books) {
            out.println(String.format(BOOK_ROW, book.getBookId(), fit(book.getTitle(), 26),
                    fit(book.getAuthor(), 18), fit(book.getCategory(), 14), book.getIsbn(),
                    book.getPublicationYear(), book.getAvailabilityStatus()));
        }
        out.println("-".repeat(header.length()));
        out.println(books.size() + " book(s) listed.");
    }

    private void printStudentTable(List<Student> students) {
        printHeading("REGISTERED STUDENTS");
        if (students.isEmpty()) {
            out.println("No students are registered yet.");
            return;
        }
        String header = String.format(STUDENT_ROW, "ID", "Name", "Email", "Contact", "Department", "Year");
        out.println(header);
        out.println("-".repeat(header.length()));
        for (Student student : students) {
            out.println(String.format(STUDENT_ROW, student.getUserId(), fit(student.getName(), 22),
                    fit(student.getEmail(), 28), student.getContactNumber(),
                    fit(student.getDepartment(), 20), student.getYearOfStudy()));
        }
        out.println("-".repeat(header.length()));
        out.println(students.size() + " student(s) registered.");
    }

    private void printRecordTable(String title, List<BorrowRecord> records) {
        FineService fines = library.getFineService();
        printHeading(title);
        if (records.isEmpty()) {
            out.println("No records found.");
            return;
        }
        String header = String.format(RECORD_ROW, "TXN", "Student", "Book", "Borrowed", "Due", "Returned", "Fine", "Status");
        out.println(header);
        out.println("-".repeat(header.length()));
        for (BorrowRecord record : records) {
            out.println(String.format(RECORD_ROW, record.getTransactionLabel(),
                    fit(record.getStudentId() + " " + record.getStudentName(), 18),
                    fit(record.getBookId() + " " + record.getBookTitle(), 24),
                    record.getBorrowDate(), record.getDueDate(),
                    record.getReturnDate() == null ? "-" : record.getReturnDate().toString(),
                    fines.formatAmount(record.getFineAmount()), record.getStatus()));
        }
        out.println("-".repeat(header.length()));
        out.println(records.size() + " record(s) listed.");
    }

    // =====================================================================
    // Input helpers
    // =====================================================================

    private String readLine() {
        if (!scanner.hasNextLine()) {
            throw new InputClosedException();
        }
        return scanner.nextLine();
    }

    private String prompt(String label) {
        out.print(label + ": ");
        out.flush();
        return readLine().trim();
    }

    private int promptInt(String label) throws InvalidInputException {
        return InputValidator.parseInt(prompt(label), label);
    }

    private boolean promptYesNo(String question) {
        String answer = prompt(question + " (y/n)");
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }

    /** Reads a menu number and checks that it lies within the allowed range. */
    private int readMenuChoice(int min, int max) throws InvalidInputException {
        int choice = InputValidator.parseInt(prompt("Enter your choice"), "Menu choice");
        if (choice < min || choice > max) {
            throw new InvalidInputException("Invalid menu choice " + choice + ". Please enter a number from "
                    + min + " to " + max + ".");
        }
        return choice;
    }

    // =====================================================================
    // Output helpers
    // =====================================================================

    private void printHeading(String title) {
        out.println();
        out.println(DOUBLE_LINE);
        out.println(title);
        out.println(DOUBLE_LINE);
    }

    private void printSuccess(String message) {
        out.println("[SUCCESS] " + message);
    }

    private void printError(String message) {
        out.println("[ERROR] " + message);
        out.println();
    }

    private void printField(String label, Object value) {
        out.printf("  %-26s: %s%n", label, value);
    }

    /** Shortens text with "..." so table columns stay aligned. */
    private static String fit(String text, int width) {
        return text.length() <= width ? text : text.substring(0, width - 3) + "...";
    }
}
