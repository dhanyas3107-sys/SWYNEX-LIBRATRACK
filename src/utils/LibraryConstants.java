package utils;

/** Central place for every rule value and file name, so nothing is hard-coded elsewhere. */
public final class LibraryConstants {

    private LibraryConstants() {
        // constants holder - never instantiated
    }

    // ----- Library rules -----
    public static final int MAX_ACTIVE_BORROWS = 3;
    public static final int LOAN_PERIOD_DAYS = 14;
    public static final int FINE_PER_DAY = 5;
    /** Rupee sign written as a Unicode escape so the source compiles on any platform encoding. */
    public static final String CURRENCY_SYMBOL = "₹";

    // ----- Validation limits -----
    public static final int MAX_TEXT_LENGTH = 100;
    public static final int MIN_PUBLICATION_YEAR = 1000;
    public static final int MIN_YEAR_OF_STUDY = 1;
    public static final int MAX_YEAR_OF_STUDY = 6;

    // ----- Identifiers -----
    public static final int FIRST_TRANSACTION_ID = 1001;

    // ----- Storage -----
    public static final String DATA_DIRECTORY = "data";
    public static final String BOOKS_FILE = "books.txt";
    public static final String STUDENTS_FILE = "students.txt";
    public static final String BORROW_RECORDS_FILE = "borrow_records.txt";

    // ----- Default librarian (staff user shipped with the system) -----
    public static final int DEFAULT_LIBRARIAN_ID = 9001;
    public static final String DEFAULT_LIBRARIAN_NAME = "Anitha Raman";
    public static final String DEFAULT_LIBRARIAN_EMAIL = "librarian@libratrack.edu";
    public static final String DEFAULT_LIBRARIAN_CONTACT = "9876500001";
    public static final String DEFAULT_LIBRARIAN_EMPLOYEE_ID = "LIB-001";
    public static final String DEFAULT_LIBRARIAN_DESIGNATION = "Head Librarian";
}
