package services;

import exceptions.BookNotFoundException;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.OperationNotAllowedException;
import models.Book;
import utils.InputValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Owns the book catalogue.
 *
 * <p>Two collections are kept in step: an {@link ArrayList} preserves insertion
 * order for listings, and a {@link HashMap} gives O(1) lookup by book ID.</p>
 */
public class BookService {

    /** Field to search when using {@link #searchBooks(String, SearchBy)}. */
    public enum SearchBy { TITLE, AUTHOR }

    private final List<Book> books = new ArrayList<>();
    private final Map<Integer, Book> bookMap = new HashMap<>();

    // ----- Create / read -----

    public void addBook(Book book) throws DuplicateRecordException {
        if (bookMap.containsKey(book.getBookId())) {
            throw new DuplicateRecordException("A book with ID " + book.getBookId() + " already exists ("
                    + bookMap.get(book.getBookId()).getTitle() + "). Book IDs must be unique.");
        }
        books.add(book);
        bookMap.put(book.getBookId(), book);
    }

    public boolean bookIdExists(int bookId) {
        return bookMap.containsKey(bookId);
    }

    public Book getBookById(int bookId) throws BookNotFoundException {
        Book book = bookMap.get(bookId);
        if (book == null) {
            throw new BookNotFoundException(bookId);
        }
        return book;
    }

    public Optional<Book> findBookById(int bookId) {
        return Optional.ofNullable(bookMap.get(bookId));
    }

    public List<Book> getAllBooks() {
        return Collections.unmodifiableList(books);
    }

    public List<Book> getAvailableBooks() {
        List<Book> result = new ArrayList<>();
        for (Book book : books) {
            if (book.isAvailable()) {
                result.add(book);
            }
        }
        return result;
    }

    public List<Book> getBorrowedBooks() {
        List<Book> result = new ArrayList<>();
        for (Book book : books) {
            if (!book.isAvailable()) {
                result.add(book);
            }
        }
        return result;
    }

    public int getTotalBooks() {
        return books.size();
    }

    public int getAvailableCount() {
        return getAvailableBooks().size();
    }

    public int getBorrowedCount() {
        return getBorrowedBooks().size();
    }

    // ----- Overloaded search methods (compile-time polymorphism) -----

    /** Search by book ID: returns a list with zero or one book. */
    public List<Book> searchBooks(int bookId) {
        Book book = bookMap.get(bookId);
        return book == null ? new ArrayList<>() : new ArrayList<>(List.of(book));
    }

    /** Case-insensitive "contains" search on the chosen field (title or author). */
    public List<Book> searchBooks(String keyword, SearchBy field) throws InvalidInputException {
        String needle = requireKeyword(keyword, field == SearchBy.AUTHOR ? "Author" : "Title");
        List<Book> result = new ArrayList<>();
        for (Book book : books) {
            String haystack = field == SearchBy.AUTHOR ? book.getAuthor() : book.getTitle();
            if (haystack.toLowerCase(Locale.ROOT).contains(needle)) {
                result.add(book);
            }
        }
        return result;
    }

    /** Narrow search: the title AND the author must both match. */
    public List<Book> searchBooks(String title, String author) throws InvalidInputException {
        String titleNeedle = requireKeyword(title, "Title");
        String authorNeedle = requireKeyword(author, "Author");
        List<Book> result = new ArrayList<>();
        for (Book book : books) {
            if (book.getTitle().toLowerCase(Locale.ROOT).contains(titleNeedle)
                    && book.getAuthor().toLowerCase(Locale.ROOT).contains(authorNeedle)) {
                result.add(book);
            }
        }
        return result;
    }

    private static String requireKeyword(String keyword, String fieldName) throws InvalidInputException {
        if (InputValidator.isBlank(keyword)) {
            throw new InvalidInputException("Please enter some text to search for in the " + fieldName + ".");
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    // ----- Update / delete -----

    /**
     * Updates a book. A null or blank argument means "keep the current value".
     * Every new value is validated first, so a bad value never leaves the book half-updated.
     */
    public Book updateBook(int bookId, String title, String author, String category,
                           String isbn, Integer publicationYear)
            throws BookNotFoundException, InvalidInputException {
        Book book = getBookById(bookId);

        String newTitle = InputValidator.isBlank(title) ? null : InputValidator.requireText(title, "Title");
        String newAuthor = InputValidator.isBlank(author) ? null : InputValidator.requireText(author, "Author");
        String newCategory = InputValidator.isBlank(category) ? null : InputValidator.requireText(category, "Category");
        String newIsbn = InputValidator.isBlank(isbn) ? null : InputValidator.validateIsbn(isbn);
        Integer newYear = publicationYear == null ? null : InputValidator.validatePublicationYear(publicationYear);

        if (newTitle != null) {
            book.setTitle(newTitle);
        }
        if (newAuthor != null) {
            book.setAuthor(newAuthor);
        }
        if (newCategory != null) {
            book.setCategory(newCategory);
        }
        if (newIsbn != null) {
            book.setIsbn(newIsbn);
        }
        if (newYear != null) {
            book.setPublicationYear(newYear);
        }
        return book;
    }

    /** Removes a book from both collections. A borrowed book can never be removed. */
    public Book removeBook(int bookId) throws BookNotFoundException, OperationNotAllowedException {
        Book book = getBookById(bookId);
        if (!book.isAvailable()) {
            throw new OperationNotAllowedException("\"" + book.getTitle() + "\" (Book ID " + bookId
                    + ") is currently borrowed and cannot be removed. Return it first.");
        }
        books.remove(book);
        bookMap.remove(bookId);
        return book;
    }
}
