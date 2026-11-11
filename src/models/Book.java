package models;

import exceptions.BookNotAvailableException;
import exceptions.InvalidInputException;
import utils.InputValidator;

/**
 * A book in the library catalogue (ENCAPSULATION).
 *
 * <p>All fields are private. The ID is final, and availability can only change
 * through {@link #markBorrowed()} and {@link #markReturned()}, so it can never
 * be flipped arbitrarily from outside.</p>
 */
public class Book {

    private final int bookId;
    private String title;
    private String author;
    private String category;
    private String isbn;
    private int publicationYear;
    private boolean available;

    /** Creates a new, available book. */
    public Book(int bookId, String title, String author, String category,
                String isbn, int publicationYear) throws InvalidInputException {
        this(bookId, title, author, category, isbn, publicationYear, true);
    }

    /** Full constructor, also used when rebuilding a book from the data file. */
    public Book(int bookId, String title, String author, String category,
                String isbn, int publicationYear, boolean available) throws InvalidInputException {
        this.bookId = InputValidator.validateId(bookId, "Book ID");
        this.title = InputValidator.requireText(title, "Title");
        this.author = InputValidator.requireText(author, "Author");
        this.category = InputValidator.requireText(category, "Category");
        this.isbn = InputValidator.validateIsbn(isbn);
        this.publicationYear = InputValidator.validatePublicationYear(publicationYear);
        this.available = available;
    }

    // ----- Getters -----

    public int getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getAvailabilityStatus() {
        return available ? "Available" : "Borrowed";
    }

    // ----- Validated setters (no setter for ID or availability) -----

    public void setTitle(String title) throws InvalidInputException {
        this.title = InputValidator.requireText(title, "Title");
    }

    public void setAuthor(String author) throws InvalidInputException {
        this.author = InputValidator.requireText(author, "Author");
    }

    public void setCategory(String category) throws InvalidInputException {
        this.category = InputValidator.requireText(category, "Category");
    }

    public void setIsbn(String isbn) throws InvalidInputException {
        this.isbn = InputValidator.validateIsbn(isbn);
    }

    public void setPublicationYear(int publicationYear) throws InvalidInputException {
        this.publicationYear = InputValidator.validatePublicationYear(publicationYear);
    }

    // ----- Controlled state changes -----

    /** Marks the book as issued; refuses if it is already issued. */
    public void markBorrowed() throws BookNotAvailableException {
        if (!available) {
            throw new BookNotAvailableException(bookId, title);
        }
        available = false;
    }

    /** Marks the book as back on the shelf (safe to call more than once). */
    public void markReturned() {
        available = true;
    }

    @Override
    public String toString() {
        return "Book[" + bookId + ", " + title + " by " + author + ", " + getAvailabilityStatus() + "]";
    }
}
