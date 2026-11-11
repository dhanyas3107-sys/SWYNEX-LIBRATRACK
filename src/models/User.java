package models;

import exceptions.InvalidInputException;
import utils.InputValidator;

/**
 * Abstract parent of every person in the system (ABSTRACTION + INHERITANCE).
 *
 * <p>It owns the data and validation shared by all users, while the abstract
 * methods force each subclass to describe its own role and borrowing rights.</p>
 */
public abstract class User {

    private final int userId;           // immutable - an ID never changes once assigned
    private String name;
    private String email;
    private String contactNumber;

    protected User(int userId, String name, String email, String contactNumber) throws InvalidInputException {
        this.userId = InputValidator.validateId(userId, "User ID");
        this.name = InputValidator.requireText(name, "Name");
        this.email = InputValidator.validateEmail(email);
        this.contactNumber = InputValidator.validateContactNumber(contactNumber);
    }

    // ----- Abstract behaviour every user type must define -----

    /** A human-readable description of what this kind of user is and can do. */
    public abstract String displayRole();

    /** Short machine-friendly label for the user type, e.g. STUDENT or LIBRARIAN. */
    public abstract String getUserType();

    /** How many books this kind of user may hold at the same time. */
    public abstract int getBorrowLimit();

    // ----- Shared behaviour -----

    /** One-line summary of the common fields; subclasses extend it via super.getDetails(). */
    public String getDetails() {
        return "ID: " + userId + " | Name: " + name + " | Email: " + email + " | Contact: " + contactNumber;
    }

    // ----- Getters (the ID has no setter by design) -----

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    // ----- Validated setters -----

    public void setName(String name) throws InvalidInputException {
        this.name = InputValidator.requireText(name, "Name");
    }

    public void setEmail(String email) throws InvalidInputException {
        this.email = InputValidator.validateEmail(email);
    }

    public void setContactNumber(String contactNumber) throws InvalidInputException {
        this.contactNumber = InputValidator.validateContactNumber(contactNumber);
    }

    @Override
    public String toString() {
        return getUserType() + "[" + userId + ", " + name + "]";
    }
}
