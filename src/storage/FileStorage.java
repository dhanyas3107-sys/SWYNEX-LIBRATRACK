package storage;

import exceptions.InvalidInputException;
import exceptions.LibraryException;
import exceptions.StorageException;
import models.Book;
import models.BorrowRecord;
import models.Student;
import utils.InputValidator;
import utils.LibraryConstants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads and writes the three data files (books, students, borrow records).
 *
 * <p>Format: one record per line, fields separated by {@code |}. Backslash, pipe and
 * line breaks inside a field are escaped ({@code \\}, {@code \p}, {@code \n}, {@code \r}),
 * so titles and names containing any special character can never break the format.
 * Lines starting with {@code #} are comments. A malformed line is skipped with a
 * warning instead of aborting the whole load.</p>
 *
 * <p>Saves go to a temporary file first and are then moved over the real file, so a
 * crash mid-write cannot leave a half-written data file.</p>
 */
public class FileStorage {

    private static final String SEPARATOR = "|";
    private static final String SPLIT_REGEX = "\\|";
    private static final String COMMENT_PREFIX = "#";

    private static final int BOOK_FIELDS = 7;
    private static final int STUDENT_FIELDS = 6;
    private static final int RECORD_FIELDS = 10;

    private final Path dataDirectory;
    private final Path booksFile;
    private final Path studentsFile;
    private final Path recordsFile;
    private final List<String> warnings = new ArrayList<>();

    /** One line of a data file, already split into unescaped fields. */
    @FunctionalInterface
    private interface LineParser<T> {
        T parse(String[] fields) throws LibraryException;
    }

    public FileStorage(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.booksFile = dataDirectory.resolve(LibraryConstants.BOOKS_FILE);
        this.studentsFile = dataDirectory.resolve(LibraryConstants.STUDENTS_FILE);
        this.recordsFile = dataDirectory.resolve(LibraryConstants.BORROW_RECORDS_FILE);
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public void clearWarnings() {
        warnings.clear();
    }

    // ----- Setup -----

    /** Creates the data directory and any missing data file (empty, with a header comment). */
    public void initialise() throws StorageException {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new StorageException("Could not create data folder '" + dataDirectory + "': " + e.getMessage(), e);
        }
        if (Files.notExists(booksFile)) {
            saveBooks(new ArrayList<>());
        }
        if (Files.notExists(studentsFile)) {
            saveStudents(new ArrayList<>());
        }
        if (Files.notExists(recordsFile)) {
            saveRecords(new ArrayList<>());
        }
    }

    // ----- Loading -----

    public List<Book> loadBooks() throws StorageException {
        return loadFile(booksFile, BOOK_FIELDS, f -> new Book(
                InputValidator.parseInt(f[0], "Book ID"), f[1], f[2], f[3], f[4],
                InputValidator.parseInt(f[5], "Publication year"),
                Boolean.parseBoolean(f[6].trim())));
    }

    public List<Student> loadStudents() throws StorageException {
        return loadFile(studentsFile, STUDENT_FIELDS, f -> new Student(
                InputValidator.parseInt(f[0], "Student ID"), f[1], f[2], f[3], f[4],
                InputValidator.parseInt(f[5], "Year of study")));
    }

    public List<BorrowRecord> loadRecords() throws StorageException {
        return loadFile(recordsFile, RECORD_FIELDS, f -> {
            LocalDate returnDate = f[7].isBlank() ? null : InputValidator.parseDate(f[7], "Return date");
            return new BorrowRecord(
                    InputValidator.parseInt(f[0], "Transaction ID"),
                    InputValidator.parseInt(f[1], "Student ID"), f[2],
                    InputValidator.parseInt(f[3], "Book ID"), f[4],
                    InputValidator.parseDate(f[5], "Borrowing date"),
                    InputValidator.parseDate(f[6], "Due date"),
                    returnDate,
                    InputValidator.parseInt(f[8], "Fine amount"),
                    parseStatus(f[9]));
        });
    }

    private <T> List<T> loadFile(Path file, int expectedFields, LineParser<T> parser) throws StorageException {
        List<T> items = new ArrayList<>();
        if (Files.notExists(file)) {
            return items;   // a missing file simply means "no data yet"
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith(COMMENT_PREFIX)) {
                    continue;
                }
                try {
                    String[] fields = splitLine(line);
                    if (fields.length != expectedFields) {
                        throw new InvalidInputException("expected " + expectedFields
                                + " fields but found " + fields.length);
                    }
                    items.add(parser.parse(fields));
                } catch (LibraryException | RuntimeException e) {
                    warnings.add(file.getFileName() + " line " + lineNumber + " was skipped: " + e.getMessage());
                }
            }
        } catch (CharacterCodingException e) {
            throw new StorageException("'" + file + "' is not a valid UTF-8 text file. "
                    + "Re-save it as UTF-8 or delete it to start fresh.", e);
        } catch (IOException e) {
            throw new StorageException("Could not read '" + file + "': " + e.getMessage(), e);
        }
        return items;
    }

    private static BorrowRecord.Status parseStatus(String text) throws InvalidInputException {
        try {
            return BorrowRecord.Status.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("unknown transaction status '" + text + "'");
        }
    }

    // ----- Saving -----

    public void saveBooks(List<Book> books) throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("# LibraTrack books: bookId|title|author|category|isbn|publicationYear|available");
        for (Book b : books) {
            lines.add(join(String.valueOf(b.getBookId()), b.getTitle(), b.getAuthor(), b.getCategory(),
                    b.getIsbn(), String.valueOf(b.getPublicationYear()), String.valueOf(b.isAvailable())));
        }
        writeAtomically(booksFile, lines);
    }

    public void saveStudents(List<Student> students) throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("# LibraTrack students: studentId|name|email|contact|department|yearOfStudy");
        for (Student s : students) {
            lines.add(join(String.valueOf(s.getUserId()), s.getName(), s.getEmail(), s.getContactNumber(),
                    s.getDepartment(), String.valueOf(s.getYearOfStudy())));
        }
        writeAtomically(studentsFile, lines);
    }

    public void saveRecords(List<BorrowRecord> records) throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add("# LibraTrack borrow records: transactionId|studentId|studentName|bookId|bookTitle"
                + "|borrowDate|dueDate|returnDate|fineAmount|status");
        for (BorrowRecord r : records) {
            lines.add(join(String.valueOf(r.getTransactionId()), String.valueOf(r.getStudentId()),
                    r.getStudentName(), String.valueOf(r.getBookId()), r.getBookTitle(),
                    r.getBorrowDate().toString(), r.getDueDate().toString(),
                    r.getReturnDate() == null ? "" : r.getReturnDate().toString(),
                    String.valueOf(r.getFineAmount()), r.getStatus().name()));
        }
        writeAtomically(recordsFile, lines);
    }

    /** Writes to "<name>.tmp" and then moves it over the target so the real file is never half-written. */
    private void writeAtomically(Path target, List<String> lines) throws StorageException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Could not write '" + target.getFileName() + "': " + e.getMessage(), e);
        } finally {
            try {
                Files.deleteIfExists(temp);   // clean up the temp file if the move did not happen
            } catch (IOException ignored) {
                // nothing more can be done; the leftover .tmp file is harmless and ignored by the app
            }
        }
    }

    // ----- Field escaping -----

    private static String join(String... fields) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(escape(fields[i]));
        }
        return builder.toString();
    }

    static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> builder.append("\\\\");
                case '|' -> builder.append("\\p");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                default -> builder.append(c);
            }
        }
        return builder.toString();
    }

    static String unescape(String value) throws InvalidInputException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\') {
                builder.append(c);
                continue;
            }
            if (i + 1 >= value.length()) {
                throw new InvalidInputException("dangling escape character");
            }
            char next = value.charAt(++i);
            switch (next) {
                case '\\' -> builder.append('\\');
                case 'p' -> builder.append('|');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                default -> throw new InvalidInputException("unknown escape sequence \\" + next);
            }
        }
        return builder.toString();
    }

    private static String[] splitLine(String line) throws InvalidInputException {
        String[] raw = line.split(SPLIT_REGEX, -1);   // -1 keeps trailing empty fields
        String[] fields = new String[raw.length];
        for (int i = 0; i < raw.length; i++) {
            fields[i] = unescape(raw[i]);
        }
        return fields;
    }
}
